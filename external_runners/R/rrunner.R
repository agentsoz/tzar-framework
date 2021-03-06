# Wrapper script for the java RRunner to call.
# Parses a json file containing parameters and assigns
# them to global variables. Note that this is pretty hacky
# and we should instead put them into a dictionary in global scope.
# This script takes two mandatory command-line arguments:
# --paramfile, and --rscript, which are the json file containing the
# parameters, and the rscript to be executed, respectively.

#-------------------------------------------------------------------------------
#
#  History: 
#
#   - Created by River Satya.
#
#   - Modified by Ascelin Gordon
#
#   - 2015 02 13 - Modified by Bill Langford
#       - Fixed the way the tryCatch handles errors so that it will 
#         properly print error messages when there is a crash, plus it 
#         will print the traceback of the stack.
#       - Before this change, an error would produce the same odd error 
#         message no matter what the error was.  The message would say 
#         something about cat not being able to print a list.  This was 
#         due to a cat statement in the error branch of the tryCatch 
#         trying to print the error condition object, which appears to be 
#         a list.  The "print" statement can do this but "cat" can't do it.
#         The fix here completely changes the way all that is handled 
#         because it was useful to get the traceback, etc.
#
#   - 2015 02 25 - Modified by Bill Langford
#       - My earlier fix worked fine when there WAS an error but not when 
#         everything ran correctly.  It would run to completion and tzar  
#         would say that it had succeeded, but the log would contain an 
#         error message at the end about "if (failed_in____RRunner)".  
#         While this didn't hurt anything, it was just not right and it 
#         was confusing when you looked at the log.
#
#-------------------------------------------------------------------------------

    #--------------------------------------------------------------------------
    # Define a function to check if a package is installed and perfrom a
    # personal install if it is not (this function uses a personal install to 
    # avoid issue with not having permissions for a system install).
    #--------------------------------------------------------------------------


install.if.required <- function(pkg) {
    if (!pkg %in% installed.packages()) {
        cat( "\n *** Note: package", pkg,
            "required by rrunner.R is not installed. Trying to install now." )
        local.lib.path <- Sys.getenv("R_LIBS_USER") # This is the default place for a personal install
        if( !file.exists(local.lib.path) ) dir.create(local.lib.path, recursive=TRUE)
        cat("\n *** Installing packages to", local.lib.path, "\n\n" )
        install.packages(pkg, lib=local.lib.path, dependencies=TRUE, repos='http://cran.r-project.org')
    }
}

   #--------------------------------------------------------------
   # Install packages required by rrunner.R (if required)
   #--------------------------------------------------------------

install.if.required( "optparse" )
install.if.required( "rjson" )

# If new libraries were added to the R_LIBS_USER location (the personal
# install directory), then add this location to the .libPaths,
# which tells R where to look for libraries
if( file.exists( Sys.getenv("R_LIBS_USER") ) ) .libPaths( c(.libPaths(), Sys.getenv("R_LIBS_USER")) )


   #--------------------------------------------------------------
   # Parse the command-line arguments 
   #--------------------------------------------------------------

library("optparse")
cmd_args <- commandArgs(TRUE)
option_list <- list(
    make_option("--paramfile"),
    make_option("--inputpath"),
    make_option("--outputpath"),
    make_option("--rscript")
)
args <- parse_args(OptionParser(option_list = option_list), args = c(cmd_args))


   #--------------------------------------------------------------
   # Parse the json file containg project info (variables, output  
   # paths etc) to an object called "tzar" 
   #--------------------------------------------------------------

library("rjson")
tzar <- fromJSON(paste(readLines(args$paramfile, warn=FALSE), collapse=""))
inputpath = args$inputpath
outputpath = args$outputpath

# Make an object called "parameters" containing all the variables in the json file
# in the R script, variables can then be accessed via parameters$PARAMETER.NAME in the R script
parameters <- tzar$parameters

# for debugging: prints out the parameters
# str(tzar)

    #--------------------------------------------------------------------------
    #  Setting the default error handler to get the traceback since 
    #  it doesn't seem possible to get the traceback from inside the tryCatch 
    #  (see comments in tryCatch below for more detail).
    #
    #  2015 02 27 - BTL
    #  I have also tried a million variants of calling either dump.frames() 
    #  or recover() in the "options(error = ..." below because they're 
    #  supposed to allow you to dump the stack frames to disk and open an 
    #  R debugger on them to see the values of the variables.  However, 
    #  I could never get it to work correctly without dumping the values 
    #  into the directory where the code was running rather than the tzar 
    #  output area.  Even dumping to the source directory and moving it to 
    #  the tzar output area didn't produce correct debug behavior on the 
    #  resulting .rda file generated by dump.frames.  recover() is supposed 
    #  to default to dumping a "last.dump.rda" file when crashing in 
    #  non-interactive mode, but it never seemed to generate any .rda file, 
    #  plus, it did not return a failure to tzar.
    #  So, I give up.  If someone can figure out how to get dump.frames to 
    #  work correctly so that you can open the resulting .rda file and 
    #  access the variables in the debugger, that would be very useful.  
    #  At the moment, I can dump the frames and load them in the debugger, 
    #  but you can't see any of the project's variables in any of the frames.
    #  All you can see are various internal R variables.
    #--------------------------------------------------------------------------

options(error = 
            function(c) 
                { 
                message ("\n\n-----  FATAL ERROR IN R CODE:  See error msgs above and traceback below...  -----: \n"); 
                traceback(2); 
                stop()
                }
        )

   #--------------------------------------------------------------
   # Source the project's R script
   #--------------------------------------------------------------

tryCatch( 
    {
    source (args$rscript)
    },

        #  BTL - 2015 02 13
        #  Removed both the warning and the error branches of this tryCatch.
        #  The warning part was unnecessary since R will spit those out 
        #  automatically.
        #  The error part had to be handled by the global "options(error = " 
        #  statement to get the traceback included in the error output.  
        #  For some reason, traceback is not accessible inside a tryCatch: 
        #  the R help for traceback says:
        #      "Errors which are caught via try or tryCatch do not generate 
        #       a traceback, so what is printed is the call sequence for the 
        #       last uncaught error, and not necessarily for the last error."

    finally = {

          #--------------------------------------------------------------
          # Write info to the tzar output dir irrespective of whether the
          # script succeeds or fails
          #--------------------------------------------------------------

        # Write a version of the parameters to a file that can be sourced directly in R 
        dump( c('parameters'), paste( outputpath, '/metadata/parameters.R', sep='') )

        # Dump the output of R's sessionInfo() command to a file in the output dir
        si <- paste( outputpath, '/metadata/R_sessionInfo.txt', sep='')
        writeLines(capture.output(date(), cat("\n"), sessionInfo()), con=si)
    }
)

