#!/usr/bin/env python
import argparse
import os
import textwrap
import subprocess

"""
Extracts the embedded X509 certs from a SAML metdata file and creates a java keystore.
This keystore is used to validate signed SAML requests.
"""

def main():
    parser = argparse.ArgumentParser(prog="saml2keystore")
    parser.add_argument("metadata", help="The SAML metadata file")

    args = parser.parse_args()
    parse(args.metadata)

def parse(path):
    working_dir = os.path.dirname(path) if len(os.path.dirname(path)) else os.path.realpath(os.getcwd())
    print working_dir
    cert_file = working_dir + "/saml.cert"
    with open(cert_file, "w") as fpw:
        with open(path) as fp:
            for line in fp.xreadlines():
                line = line.strip()
                if "X509Certificate" in line:
                    cert = line.split(">")[1].split("<")[0]
                    fpw.write("-----BEGIN CERTIFICATE-----\n")
                    fpw.write(textwrap.fill(cert, 64))
                    fpw.write("\n-----END CERTIFICATE-----\n")

    keystore_file = working_dir + "/keystore.jks"
    os.remove(keystore_file)
    print "Create a private key for signing our requests."
    print "Make the key and store password: zorroa"
    cmd = [
        "keytool",
        "-genkey",
        "-v",
        "-keystore",
        keystore_file,
        "-alias",
        "zorroa",
        "-keyalg",
        "RSA",
        "-keysize",
        "2048",
        "-validity",
        "36500"]
    subprocess.call(cmd)

    print "Adding the IDP public keys"
    cmd = [
        "keytool",
        "-import",
        "-alias",
        "idp",
        "-file",
        cert_file,
        "-keystore",
        keystore_file,
        "-storepass",
        "zorroa",
        "-noprompt"]
    subprocess.call(cmd)

    print "removing temp cert file"
    os.remove(cert_file)

if __name__ == '__main__':
    main()
