#!/usr/bin/env python
"""
    install_dist.py

    This script takes an already built (tar) dist package for a given 
    Archivist / Analyst and copies it up to a given AWS host.

    After copying the tar, it'll uncompress it in a specified
    directory for you.

    Example:
    install_dist.py --server_path_file 
    ~/Zorroa/repos/zorroa-server/archivist/dist/archivist-0.31.0.tar.gz
    --aws_host 52.91.27.225 --debug --verbose --aws_user computeruser

"""
import argparse
import getpass
import os
import paramiko
import sys
from scp import SCPClient
import textwrap


# ------------------------------------------------------------------------- 
# Global Variables
#
DEBUG = False
VERBOSE = False

AWS_USER = "computeruser"
AWS_PATH = "/home/computeruser"

# -------------------------------------------------------------------------
# Functions
# 

# ------------------------------
def check_dist_package(server_file):
    """
    Checks to see if we have a dist package already built and ready
    for upload.  Input is the path and file to the tar.

    Returns True if so, and False if not
    """

    if DEBUG:
        print "[DEBUG] check_dist_package"

    if not os.path.isfile(server_file):
        print "Invalid server file/path: " + str(server_file)
        return False

    if not os.access(server_file, os.R_OK):
        print "File is not readable: " + str(server_file)
        return False

    file_extension = os.path.splitext(server_file)[1]
    if (file_extension != ".gz"): 
        print "File is not a gz: " + str(server_file) + " " + \
               str(file_extension)
        print "make sure you created the dist package with --compress"
        return False

    return True


# ------------------------------ 
def check_already_installed(aws_path, aws_host, aws_user, password,
                            filename_version, filename):
    """
    check to see if the given path already has an archivist or
    analyst there.
    
    return if it does or not
    """

    # check to see if the symlink is there
    cmd = "test -L " + str(aws_path) + "/" + str(filename) 
    result = run_remote_command(cmd, aws_host, aws_user, password)

    if (result == 1):
        if DEBUG:
            print "[DEBUG] no symlink"

        # no symlink, what about the directory?
        cmd = "test -d " + str(aws_path) + "/" + str(filename_version)
        result = run_remote_command(cmd, aws_host, aws_user, password)

        if (result == 1):
            if DEBUG:
                print "[DEBUG] no directory."

            # no symlink, no dir, means this will be a new install 
            return(False)

        else:
            # no symlink, but yes dir, means it probably just lost
            # it's symlink.
            return(True)

    else:
        if DEBUG:
            print "[DEBUG] found symlink"

        cmd = "test -d " + str(aws_path) + "/" + str(filename_version)
        result = run_remote_command(cmd, aws_host, aws_user, password)

        if (result == 1):
            if DEBUG:
                print "[DEBUG] no directory."

            # yes symlink, but no dir.  nuke the symlink
            cmd = "rm -f " + str(aws_path) + "/" + str(filename)
            result = run_remote_command(cmd, aws_host, aws_user, password)

            return(False)
        else:
            if DEBUG:
                print "[DEBUG] yes directory."

            # yes sym link, and yes directory.  definitely already there.
            return(True)

# ------------------------------ 
def save_config_files(aws_path, aws_host, aws_user, password, 
        filename_version):
    """
    save any files needed for an upgrade
    return 0 if successful, 1 if error
    """

    if DEBUG:
        print "[DEBUG] saving config files"

    # copy the config files
    cmd = "cp -r " +str(aws_path) + "/" + str(filename_version) + \
          "/config" + " /tmp/zorroa"

    if DEBUG:
        print "[DEBUG] cmd: " + str(cmd)

    result = run_remote_command(cmd, aws_host, aws_user, password)
    return(result)

# ------------------------------ 
def restore_config_files(aws_path, aws_host, aws_user, password, 
        filename_version):
    """
    restore any files needed for an upgrade
    return 0 if successful, 1 if error
    """

    if DEBUG:
        print "[DEBUG] restoring config files"

    # copy the config files
    cmd = "cp -f /tmp/zorroa/* " + str(aws_path) + "/" + \
           str(filename_version) + "/config" 

    if DEBUG:
        print "[DEBUG] cmd: " + str(cmd)

    result = run_remote_command(cmd, aws_host, aws_user, password)
    return(result)


# ------------------------------ 
def rm_config_files(aws_path, aws_host, aws_user, password): 
    
    """
    remove temp files needed for an upgrade
    return 0 if successful, 1 if error
    """

    if DEBUG:
        print "[DEBUG] removing temp config files"

    # copy the config files
    cmd = "rm -rf /tmp/zorroa"

    if DEBUG:
        print "[DEBUG] cmd: " + str(cmd)

    result = run_remote_command(cmd, aws_host, aws_user, password)
    return(result)


# ------------------------------ 
def rm_file(aws_path, aws_host, aws_user, password, filename):
    """
    remove a given file we have 
    return 0 if successful, 1 if error
    """

    if DEBUG:
        print "[DEBUG] rm_file"

    # copy the config files
    cmd = "rm -f " + str(aws_path) + "/" + str(filename)

    if DEBUG:
        print "[DEBUG] cmd: " + str(cmd)
    result = run_remote_command(cmd, aws_host, aws_user, password)
    return(result)


# ------------------------------ 
def createSSHClient(server, port, user, password):
    """
    helper function that creates an SSH client
    returns the ssh client
    """

    client = paramiko.SSHClient()
    client.load_system_host_keys()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(server, port, user, password)
    return client 


# ------------------------------ 
def splitext(path): 
    """
    helper function for getting a filename 
    """

    for ext in ['.tar.gz', '.tar.bz2']:
        if path.endswith(ext):
            return path[:-len(ext)], path[-len(ext):]
    return os.path.splitext(path) 

# ------------------------------ 
def copy_file(server_file, aws_path, aws_user, aws_host, password):
    """
    Copy a file from localhost to remote host using ssh/scp
    returns 0 on success an 1 on failure
    """
    
    if DEBUG:
        print "[DEBUG] copy_file"

    try:
        ssh = createSSHClient(aws_host, 22, aws_user, password)
        scp = SCPClient(ssh.get_transport())
    except:
        if VERBOSE:
            print "Error with SCP credentials."
        return(1)

    try:
        scp.put(server_file, aws_path)
        scp.close()
        if DEBUG:
            print "[DEBUG] scp done"
        return(0)
    except:
        if DEBUG:
            print "[DEBUG] scp failed"
            print "[DEBUG] " + str(sys.exc_info()[0])
        return(1)

    return(0)


# ------------------------------ 
def run_remote_command(command, aws_host, aws_user, password):
    """ 
    Run a given command on a remote host via ssh
    TODO: should return the success/failure value of the cmd
    """

    if DEBUG:
        print "[DEBUG] run_remote_command connecting"

    if VERBOSE:
        print "Executing: {}".format(str(command))

    if DEBUG:
        print "[DEBUG] sshClient creating connection"

    try:
        ssh = createSSHClient(aws_host, 22, aws_user, password)
    except:
        if VERBOSE:
            print "Error with ssh credentials."
        return(1)
    

    try: 
        if DEBUG:
            print "[DEBUG] sshClient connecting"
        ssh.connect(hostname=aws_host, username=aws_user, password=password)

        if DEBUG:
            print "[DEBUG] sshClient exec_command"

        stdin, stdout, stderr = ssh.exec_command(str(command))
        exit_status = stdout.channel.recv_exit_status()
        if VERBOSE:
            for line in stdout:
                print '[stdout] ' + line.strip('\n') 

        ssh.close()
        if DEBUG:
            print "[DEBUG] command completed exit status: " + \
                   str(exit_status)
        return(exit_status)
    except:
        if DEBUG:
            print "[DEBUG] error in run_remote_command ssh.connect"
        return(1)


# ------------------------------ 
def do_print_help():
    """
    help message when --help is passed
    """
    print textwrap.dedent("""\

What does this script do:
  1) It uses ssh to log into AWS_HOST to see if software is
     already installed.  Unless you specify AWS_PATH, it'll
     use a default AWS_PATH (/home/computeruser)

     The software is expected to be following the naming
     convention from package.py:  archivist-0.32.0.tar.gz

  2) If the software is already there, we copy the files in
     the config directory to /tmp (we copy the files back
     after install.

  3) We copy the server_path_file to the AWS_HOST and put it
     in the AWS_PATH directory.  
 
  4) We uncompress the server_path_file on the AWS_HOST.
  5) We create a symlink pointing to the directory.


-----------------------------------------------------------------


Some examples of usage:

 install_dist.py 
     --server_path_file
       ~/repos/zorroa-server/archivist/dist/archivist-0.32.0.tar.gz
    --aws_host 52.91.27.225
    --verbose
    --debug 

 * this copies archivist-0.32.0.tar.gz to AWS_PATH 
   (default is /home/computeruser)
 * uncompresses it there, and makes a symlink for 'archivist' to 
   point to 'archivist-0.32.0'



 install_dist.py
     --server_path_file
       ~/repos/zorroa-server/archivist/dist/archivist-0.32.0.tar.gz
    --aws_host 52.91.27.225
    --aws_path /tmp
    --aws_user bob
    --verbose
    --debug 


 * instead of the default path on the aws_host (/home/computeruser), 
   use /tmp


""")





# -------------------------------------------------------------------------
# MAIN is here
#
def main():

    global DEBUG
    global VERBOSE

    # -------------------
    # Parse the arguments
    #
    parser = argparse.ArgumentParser(description='install_dist')
    parser.add_argument('--verbose', '-v',
                        action='store_true',
                        help='verbose flag')

    parser.add_argument('--debug', '-d',
                        action='store_true',
                        help='debug flag')

    parser.add_argument('--help_examples',
                        action='store_true',
                        help='print detailed help message')

    parser.add_argument('--server_path_file',
                        type=str,
                        required=True,
                        help='the tar dist package - full path')

    parser.add_argument('--aws_host',
                        type=str,
			required=True,
                        help='AWS HOST to install (IP Address)')

    parser.add_argument('--aws_user',
                        type=str,
			default=AWS_USER,
                        help='AWS user - defaults to ' + AWS_USER)

    parser.add_argument('--aws_path',
                        type=str,
			default=AWS_PATH,
                        help='AWS install path - defaults to ' + AWS_PATH)


    args = parser.parse_args() 

    if args.verbose:
        # set the global variable
        VERBOSE = args.verbose

    if args.help_examples:
        do_print_help()
        sys.exit(0)

    if args.debug:
        print "[DEBUG] VERBOSE: ", args.verbose
        print "[DEBUG] DEBUG: ", args.debug 
        print "[DEBUG] SERVER_PATH_FILE: ", args.server_path_file
        print "[DEBUG] AWS_HOST: ", args.aws_host
        print "[DEBUG] AWS_USER: ", args.aws_user
        print "[DEBUG] AWS_PATH: ", args.aws_path
        print "[DEBUG] run_server: ", args.run_server

        DEBUG = args.debug

    # --------------------------------------- 
    # check to see if we have a dist package.
    result = check_dist_package(args.server_path_file)
    if VERBOSE:
        print "check_dist_package result: " + str(result)

    if (result == False):
        print "returned False from check_dist_package.  exiting"
        sys.exit(1)


    # --------------------------------------- 
    # get the password, we prompt instead of hardcode
    # and do some general stuff
    print "Please input the password for the " + str(args.aws_user) + \
            " user:"
    password = getpass.getpass()

    # this will not work on windows
    server_file = os.path.basename(args.server_path_file)

    # lets get the version info, and filename
    filename_version = splitext(server_file)[0]
    filename, version = filename_version.split('-')


    # --------------------------------------- 
    # check to see if we have software already installed
    # in the given path.  (upgrade) 
    upgrade_result = check_already_installed(args.aws_path, 
                                             args.aws_host,
                                             args.aws_user, 
                                             password,
                                             filename_version, 
                                             filename)

    if (upgrade_result == True):
        if VERBOSE:
            print "Existing Zorroa Server software!  Do upgrade"

        # first remove the symlink
        result = rm_file(args.aws_path, args.aws_host, args.aws_user, 
                         password, filename)

        # preserve the configs
        result = save_config_files(args.aws_path, args.aws_host,
                                   args.aws_user, password,
                                   filename_version)
    else:
        if VERBOSE:
            print "New Zorroa Server Software install"

        
    # --------------------------------------- 
    # copy the file
    result = copy_file(args.server_path_file, args.aws_path, args.aws_user,
                       args.aws_host, password)

    if VERBOSE:
        print "copy_file result: " + str(result)

    if (result >= 1):
        print "returned error from copy_file.  exiting"
        sys.exit(1)


    # --------------------------------------- 
    # take the dist package, and uncompress it
    cmd = "tar xvfpz " + args.aws_path + "/" + server_file + " -C " + \
          args.aws_path
    result = run_remote_command(cmd, args.aws_host, args.aws_user, 
                                password)
	
    if VERBOSE:
        print "run_remote_command tar result: " + str(result)


    # --------------------------------------- 
    # if it was an upgrade, restore the config files
    if (upgrade_result == True):
        # copy over the config files
        result = restore_config_files(args.aws_path, args.aws_host,
                                   args.aws_user, password,
                                   filename_version)

        # cleanup tmp dir
        result = rm_config_files(args.aws_path, args.aws_host, 
                                 args.aws_user, password)




    # --------------------------------------- 
    # lets make a simple symlink 
    cmd = "ln -s " + args.aws_path + "/" + str(filename_version) + \
          " " + args.aws_path + "/" + str(filename)
    result = run_remote_command(cmd, args.aws_host, args.aws_user, 
                                password)
	
    if VERBOSE:
        print "run_remote_command symlink result: " + str(result)




# -------------------------------------------------------------------------
# run main()
#
if __name__ == '__main__':
        main()
