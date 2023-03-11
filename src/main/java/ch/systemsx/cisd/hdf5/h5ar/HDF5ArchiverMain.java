/*
 * Copyright 2007 - 2018 ETH Zuerich, CISD and SIS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.systemsx.cisd.hdf5.h5ar;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FilenameUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;

import ch.systemsx.cisd.base.exceptions.IErrorStrategy;
import ch.systemsx.cisd.hdf5.BuildAndEnvironmentInfo;
import ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator.FileFormatVersionBounds;
import hdf.hdf5lib.exceptions.HDF5JavaException;
import hdf.hdf5lib.exceptions.HDF5LibraryException;

/**
 * The main class of the HDF5 based archiver.
 * 
 * @author Bernd Rinn
 */
public class HDF5ArchiverMain
{

    private static final String FILE_EXTENSION_H5 = ".h5";

    private static final String FILE_EXTENSION_H5AR = ".h5ar";

    private enum Command
    {
        ARCHIVE(new String[]
            { "A", "AR", "ARCHIVE" }, false), CAT(new String[]
            { "C", "CT", "CAT" }, true), EXTRACT(new String[]
            { "E", "EX", "EXTRACT" }, true), DELETE(new String[]
            { "D", "RM", "DELETE", "REMOVE" }, false), LIST(new String[]
            { "L", "LS", "LIST" }, true), VERIFY(new String[]
            { "V", "VF", "VERIFY" }, true), HELP(new String[]
            { "H", "HELP" }, true);

        String[] forms;

        boolean readOnly;

        Command(String[] forms, boolean readOnly)
        {
            this.forms = forms;
            this.readOnly = readOnly;
        }

        boolean isReadOnly()
        {
            return readOnly;
        }

        static Command parse(String commandStr)
        {
            final String commandStrU = commandStr.toUpperCase();
            for (Command cmd : values())
            {
                for (String frm : cmd.forms)
                {
                    if (frm.equals(commandStrU))
                    {
                        return cmd;
                    }
                }
            }
            return HELP;
        }
    }

    private final static IErrorStrategy ERROR_STRATEGY_CONTINUE = new IErrorStrategy()
        {
            @Override
            public void dealWithError(Throwable th) throws ArchiverException
            {
                System.err.println(th.getMessage());
            }

            @Override
            public void warning(String message)
            {
                System.err.println(message);
            }
        };

    @Argument
    private List<String> arguments;

    private Command command;

    private File archiveFile;

    private final boolean initializationOK;

    @Option(name = "-i", aliases = "--include", metaVar = "REGEX", hidden = true, usage = "Regex of files to include")
    private List<String> fileWhiteList = new ArrayList<String>();

    @Option(name = "-e", aliases = "--exclude", metaVar = "REGEX", usage = "Regex of files to exclude")
    private List<String> fileBlackList = new ArrayList<String>();

    @Option(name = "-I", aliases = "--include-dirs", metaVar = "REGEX", hidden = true, usage = "Regex of directories to include")
    private List<String> dirWhiteList = new ArrayList<String>();

    @Option(name = "-E", aliases = "--exclude-dirs", metaVar = "REGEX", hidden = true, usage = "Regex of directories to exclude")
    private List<String> dirBlackList = new ArrayList<String>();

    @Option(name = "-c", aliases = "--compress", metaVar = "REGEX", hidden = true, usage = "Regex of files to compress")
    private List<String> compressionWhiteList = new ArrayList<String>();

    @Option(name = "-nc", aliases = "--no-compression", metaVar = "REGEX", hidden = true, usage = "Regex of files not to compress")
    private List<String> compressionBlackList = new ArrayList<String>();

    @Option(name = "-C", aliases = "--compress-all", usage = "Compress all files")
    private Boolean compressAll = null;

    @Option(name = "-r", aliases = "--root-dir", metaVar = "DIR", usage = "Root directory for archiving / extracting / verifying")
    private File rootOrNull;

    @Option(name = "-D", aliases = "--suppress-directories", usage = "Supress output for directories itself for LIST and VERIFY")
    private boolean suppressDirectoryEntries = false;

    @Option(name = "-R", aliases = "--recursive", usage = "Recursive LIST and VERIFY")
    private boolean recursive = false;

    @Option(name = "-v", aliases = "--verbose", usage = "Verbose output (all operations)")
    private boolean verbose = false;

    @Option(name = "-q", aliases = "--quiet", usage = "Quiet operation (only error output)")
    private boolean quiet = false;

    @Option(name = "-n", aliases = "--numeric", usage = "Use numeric values for mode, uid and gid for LIST and VERIFY")
    private boolean numeric = false;

    @Option(name = "-t", aliases = "--test-checksums", usage = "Test CRC32 checksums of files in archive for LIST")
    private boolean testAgainstChecksums = false;

    @Option(name = "-a", aliases = "--verify-attributes", usage = "Consider file attributes for VERIFY")
    private boolean verifyAttributes = false;

    @Option(name = "-m", aliases = "--check-missing-files", usage = "Check for files present on the filesystem but missing from the archive for VERIFY")
    private boolean checkMissingFile = false;

    @Option(name = "-F", aliases = "--file-format", hidden = true, usage = "Specifies the file format version when creating an archive (N=1 -> HDF51.8 (default), N=2 -> HDF51.10), N=99 -> LATEST")
    private int fileFormat = 1;

    @Option(name = "-S", aliases = "--stop-on-error", hidden = true, usage = "Stop on first error and give detailed error report")
    private boolean stopOnError = false;

    @Option(name = "-N", aliases = "--no-sync", hidden = true, usage = "Do not sync to disk before program exits (write mode only)")
    private boolean noSync = false;

    private HDF5Archiver archiver;

    /**
     * The command line parser.
     */
    private final CmdLineParser parser = new CmdLineParser(this);

    private HDF5ArchiverMain(String[] args)
    {
        try
        {
            parser.parseArgument(args);
        } catch (CmdLineException ex)
        {
            System.err.printf("Error when parsing command line: '%s'\n", ex.getMessage());
            printHelp(true);
            initializationOK = false;
            return;
        }
        if (arguments == null || arguments.size() < 2)
        {
            printHelp(true);
            initializationOK = false;
            return;
        }
        command = Command.parse(arguments.get(0));
        if (command == null || command == Command.HELP)
        {
            printHelp(true);
            initializationOK = false;
            return;
        }
        if (arguments.get(1).endsWith(FILE_EXTENSION_H5)
                || arguments.get(1).endsWith(FILE_EXTENSION_H5AR))
        {
            archiveFile = new File(arguments.get(1));
        } else
        {
            archiveFile = new File(arguments.get(1) + FILE_EXTENSION_H5AR);
            if (command.isReadOnly() && archiveFile.exists() == false)
            {
                archiveFile = new File(arguments.get(1) + FILE_EXTENSION_H5);
                if (command.isReadOnly() && archiveFile.exists() == false)
                {
                    archiveFile = new File(arguments.get(1));
                }
            }
        }
        if (command.isReadOnly() && archiveFile.exists() == false)
        {
            System.err.println("Archive '" + archiveFile.getAbsolutePath() + "' does not exist.");
            initializationOK = false;
            return;
        }
        if (quiet && verbose)
        {
            System.err.println("Cannot be quiet and verbose at the same time.");
            initializationOK = false;
            return;
        }
        initializationOK = true;
    }

    @Option(name = "-V", aliases = "--version", hidden = true, usage = "Prints out the version information")
    void printVersion(final boolean exit)
    {
        System.err.println("HDF5 archiver version "
                + BuildAndEnvironmentInfo.INSTANCE.getFullVersion());
        if (exit)
        {
            System.exit(0);
        }
    }

    @Option(name = "-B", aliases = "--build", hidden = true, usage = "Prints out the build and environment information")
    void printBuildAndEnvironmentInfo(final boolean exit)
    {
        System.err.println(ch.systemsx.cisd.hdf5.BuildAndEnvironmentInfo.INSTANCE);
        if (exit)
        {
            System.exit(0);
        }
    }

    private boolean helpPrinted = false;

    @Option(name = "-H", aliases = "--help", hidden = true, usage = "Shows this help text")
    void printHelp(final boolean dummy)
    {
        if (helpPrinted)
        {
            return;
        }
        
        System.err.println("h5ar " +
                  "[ARCHIVE [option [...]] <archive_file> <item-to-archive> [...]\n"
                + "     | CAT [option [...]] <archive_file> <item-to-cat> [...]\n"
                + "     | EXTRACT [option [...]] <archive_file> [<item-to-unarchive> [...]]\n"
                + "     | DELETE [option [...]] <archive_file> <item-to-delete> [...]\n"
                + "     | LIST [option [...]] <archive_file> [<item-to-list> [...]]\n"
                + "     | VERIFY [option [...]] <archive_file> [<item-to-verify> [...]]]");
        System.err.println("\nOptions:");
        parser.printUsage(new OutputStreamWriter(System.err), null, OptionHandlerFilter.ALL);
        System.err.println("\nModes (command capitalization ignored):");
        System.err.println(" ARCHIVE (AR, A): add files on the file system to an archive (always recursive)");
        System.err.println(" CAT (C): extract file(s) from an archive to stdout");
        System.err.println(" EXTRACT (EX, E): extract files from an archive to the file system (always recursive)");
        System.err.println(" DELETE (REMOVE, RM, D): delete files from an archive");
        System.err.println(" LIST (LS, S): list files in an archive");
        System.err
                .println(" VERIFY (VF, V): verify the existence and integrity of files on the file system vs. the content of an archive");
        System.err.println("\nExamples:");
        System.err.println(" h5ar ar /tmp/home -r ~/ .");
        System.err.println("  - will create home.h5ar in /tmp containing all files of the user's home directory (note: ARCHIVE is always recursive).");
        System.err.println(" h5ar ls -v -R -t -S /tmp/home");
        System.err.println("  - will list the full content of archive /tmp/home.h5ar recursively, with full detail and verify the checksums; stop if there is a checksum mismatch.");
        System.err.println(" h5ar vf -r ~/ -v -R -m /tmp/home foo bar");
        System.err.println("  - will verify the content of directries foo/ and bar/ in archive /tmp/home.h5ar recursively against the user's home directory, detecting any missing files.");
        
        helpPrinted = true;
    }

    private boolean createArchiver()
    {
        final FileFormatVersionBounds fileFormatEnum;
        switch (fileFormat)
        {
            case 1:
                fileFormatEnum = FileFormatVersionBounds.V1_8_V1_8;
                break;
            case 2:
                fileFormatEnum = FileFormatVersionBounds.V1_10_V1_10;
                break;
           default:
               fileFormatEnum = FileFormatVersionBounds.LATEST_LATEST;
        }
        try
        {
            archiver =
                    new HDF5Archiver(archiveFile, command.isReadOnly(), noSync, fileFormatEnum,
                            stopOnError ? IErrorStrategy.DEFAULT_ERROR_STRATEGY
                                    : ERROR_STRATEGY_CONTINUE);
        } catch (HDF5JavaException ex)
        {
            // Problem opening the archive file: non readable / writable
            System.err.println("Error opening archive file: " + ex.getMessage());
            return false;
        } catch (HDF5LibraryException ex)
        {
            // Problem opening the archive file: corrupt file
            System.err.println("Error opening archive file: corrupt file ["
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage() + "]");
            return false;
        }
        return true;
    }

    private ArchivingStrategy createArchivingStrategy()
    {
        final ArchivingStrategy strategy =
                new ArchivingStrategy(compressionBlackList.isEmpty() ? ArchivingStrategy.DEFAULT
                        : ArchivingStrategy.DEFAULT_NO_COMPRESSION);
        if (compressAll != null)
        {
            strategy.compressAll(compressAll);
        }
        for (String pattern : fileWhiteList)
        {
            strategy.addToFileWhiteList(pattern);
        }
        for (String pattern : fileBlackList)
        {
            strategy.addToFileBlackList(pattern);
        }
        for (String pattern : dirWhiteList)
        {
            strategy.addToDirWhiteList(pattern);
        }
        for (String pattern : dirBlackList)
        {
            strategy.addToDirBlackList(pattern);
        }
        for (String pattern : fileWhiteList)
        {
            strategy.addToFileWhiteList(pattern);
        }
        for (String pattern : compressionWhiteList)
        {
            strategy.addToCompressionWhiteList(pattern);
        }
        for (String pattern : compressionBlackList)
        {
            strategy.addToCompressionBlackList(pattern);
        }
        return strategy;
    }

    private File getFSRoot()
    {
        return (rootOrNull == null) ? new File(".") : rootOrNull;
    }

    private static class ListingVisitor implements IArchiveEntryVisitor
    {
        private final boolean verifying;

        private final boolean quiet;

        private final boolean verbose;

        private final boolean numeric;

        private final boolean suppressDirectoryEntries;

        private int checkSumFailures;

        ListingVisitor(boolean verifying, boolean quiet, boolean verbose, boolean numeric)
        {
            this(verifying, quiet, verbose, numeric, false);
        }

        ListingVisitor(boolean verifying, boolean quiet, boolean verbose, boolean numeric,
                boolean suppressDirectoryEntries)
        {
            this.verifying = verifying;
            this.quiet = quiet;
            this.verbose = verbose;
            this.numeric = numeric;
            this.suppressDirectoryEntries = suppressDirectoryEntries;
        }

        @Override
        public void visit(ArchiveEntry entry)
        {
            if (suppressDirectoryEntries && entry.isDirectory())
            {
                return;
            }
            if (verifying)
            {
                final boolean ok = entry.isOK();
                if (quiet == false)
                {
                    System.out.println(entry.describeLink(verbose, numeric, true));
                }
                if (ok == false)
                {
                    System.err.println(entry.getStatus(true));
                    ++checkSumFailures;
                }
            } else
            {
                if (quiet == false)
                {
                    System.out.println(entry.describeLink(verbose, numeric, false));
                }
            }
        }

        boolean isOK(int missingFiles)
        {
            if (verifying && (checkSumFailures + missingFiles > 0))
            {
                System.err.println(checkSumFailures + missingFiles + " file(s) failed the test.");
                return false;
            } else
            {
                return true;
            }
        }
    }

    boolean run()
    {
        if (initializationOK == false)
        {
            return false;
        }
        try
        {
            switch (command)
            {
                case ARCHIVE:
                {
                    if (arguments.size() == 2)
                    {
                        System.err.println("Nothing to archive.");
                        break;
                    }
                    if (createArchiver() == false)
                    {
                        break;
                    }
                    final ArchivingStrategy strategy = createArchivingStrategy();
                    if (verbose)
                    {
                        System.out.printf("Archiving to file '%s', file system root: '%s'\n",
                                archiveFile, getFSRoot());
                    }
                    if (rootOrNull != null)
                    {
                        for (int i = 2; i < arguments.size(); ++i)
                        {
                            if (verbose)
                            {
                                System.out.printf("  Adding entry: '%s'\n", arguments.get(i));
                            }
                            archiver.archiveFromFilesystem(rootOrNull, new File(rootOrNull,
                                    arguments.get(i)), strategy,
                                    verbose ? IArchiveEntryVisitor.NONVERBOSE_VISITOR : null);
                        }
                    } else
                    {
                        for (int i = 2; i < arguments.size(); ++i)
                        {
                            if (verbose)
                            {
                                System.out.printf("  Adding entry: '%s'\n", arguments.get(i));
                            }
                            archiver.archiveFromFilesystem(new File(arguments.get(i)), strategy,
                                    true, verbose ? IArchiveEntryVisitor.NONVERBOSE_VISITOR : null);
                        }
                    }
                    break;
                }
                case CAT:
                {
                    if (createArchiver() == false)
                    {
                        break;
                    }
                    if (arguments.size() == 2)
                    {
                        System.err.println("Nothing to cat.");
                        break;
                    } else
                    {
                        for (int i = 2; i < arguments.size(); ++i)
                        {
                            archiver.extractFile(arguments.get(i), new FileOutputStream(
                                    FileDescriptor.out));
                        }
                    }
                    break;
                }
                case EXTRACT:
                {
                    if (createArchiver() == false)
                    {
                        break;
                    }
                    final ArchivingStrategy strategy = createArchivingStrategy();
                    if (verbose)
                    {
                        System.out.printf("Extracting from file '%s', file system root: '%s'\n",
                                archiveFile, getFSRoot());
                    }
                    if (arguments.size() == 2)
                    {
                        if (verbose)
                        {
                            System.out.println("  Extracting entry: '/'");
                        }
                        archiver.extractToFilesystem(getFSRoot(), "/", strategy,
                                verbose ? IArchiveEntryVisitor.DEFAULT_VISITOR : quiet ? null
                                        : IArchiveEntryVisitor.NONVERBOSE_VISITOR);
                    } else
                    {
                        for (int i = 2; i < arguments.size(); ++i)
                        {
                            if (verbose)
                            {
                                System.out.printf("  Extracting entry: '%s'\n", arguments.get(i));
                            }
                            final String unixPath =
                                    FilenameUtils.separatorsToUnix(arguments.get(i));
                            archiver.extractToFilesystem(getFSRoot(), unixPath, strategy,
                                    verbose ? IArchiveEntryVisitor.DEFAULT_VISITOR : quiet ? null
                                            : IArchiveEntryVisitor.NONVERBOSE_VISITOR);
                        }
                    }
                    break;
                }
                case DELETE:
                {
                    if (arguments.size() == 2)
                    {
                        System.err.println("Nothing to delete.");
                        break;
                    }
                    if (createArchiver() == false)
                    {
                        break;
                    }
                    if (verbose)
                    {
                        System.out.printf("Deleting from file '%s'\n", archiveFile);
                        for (String entry : arguments.subList(2, arguments.size()))
                        {
                            System.out.printf("  Deleting entry: '%s'\n", entry);
                        }
                    }
                    archiver.delete(arguments.subList(2, arguments.size()),
                            verbose ? IArchiveEntryVisitor.NONVERBOSE_VISITOR : null);
                    break;
                }
                case VERIFY:
                {
                    if (createArchiver() == false)
                    {
                        break;
                    }
                    final ListingVisitor visitor =
                            new ListingVisitor(true, quiet, verbose, numeric);
                    int missingFiles = 0;
                    if (arguments.size() == 2)
                    {
                        missingFiles = doVerify("/", visitor);
                    } else
                    {
                        for (int i = 2; i < arguments.size(); ++i)
                        {
                            missingFiles += doVerify(arguments.get(i), visitor);
                        }
                    }
                    return visitor.isOK(missingFiles);
                }
                case LIST:
                {
                    if (createArchiver() == false)
                    {
                        break;
                    }
                    final ListingVisitor visitor =
                            new ListingVisitor(testAgainstChecksums, quiet, verbose, numeric,
                                    suppressDirectoryEntries);
                    if (arguments.size() == 2)
                    {
                        doList("/", visitor);
                    } else
                    {
                        for (int i = 2; i < arguments.size(); ++i)
                        {
                            doList(arguments.get(i), visitor);
                        }
                    }
                    return visitor.isOK(0);
                }
                case HELP: // Can't happen any more at this point
                    break;
            }
            return true;
        } finally
        {
            if (archiver != null)
            {
                archiver.close();
            }
        }
    }

    private void doList(final String fileOrDir, final ListingVisitor visitor)
    {
        if (verbose)
        {
            System.out.printf("Listing entry '%s' of file '%s'\n", fileOrDir, archiveFile);
        }
        archiver.list(fileOrDir, visitor, ListParameters.build().recursive(recursive)
                .readLinkTargets(verbose).testArchive(testAgainstChecksums).get());
    }

    private int doVerify(final String fileOrDir, final ListingVisitor visitor)
    {
        if (verbose)
        {
            System.out.printf("Verifying entry '%s' of file '%s', file system root: '%s'\n",
                    fileOrDir, archiveFile, getFSRoot());
        }
        final AtomicInteger missingFileCount = new AtomicInteger();
        final IArchiveEntryVisitor missingFileVisitorOrNull =
                checkMissingFile ? new IArchiveEntryVisitor()
                    {
                        @Override
                        public void visit(ArchiveEntry entry)
                        {
                            final String errMsg =
                                    "ERROR: Object '" + entry.getName()
                                            + "' does not exist in archive.";
                            if (verbose)
                            {
                                System.out.println(entry.describeLink(true, false,
                                        false) + "\t" + errMsg);
                            } else if (quiet == false)
                            {
                                System.out.println(entry.getPath() + "\t" + errMsg);
                            }
                            System.err.println(errMsg);
                            missingFileCount.incrementAndGet();
                        }
                    } : null;
        archiver.verifyAgainstFilesystem(fileOrDir, getFSRoot(), visitor,
                missingFileVisitorOrNull, VerifyParameters.build().recursive(recursive)
                        .numeric(numeric).verifyAttributes(verifyAttributes).get());
        return missingFileCount.get();
    }

    public static void main(String[] args)
    {
        final HDF5ArchiverMain main = new HDF5ArchiverMain(args);
        if (main.run() == false)
        {
            System.exit(1);
        }
    }

}
