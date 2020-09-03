#!/usr/bin/env python3
from setuptools import setup

# See https://packaging.python.org/tutorials/packaging-projects/
# for details about packaging python projects

# Generating distribution archives (run from same directory as this file)
# python3 -m pip install --user --upgrade setuptools wheel
# python3 setup.py sdist bdist_wheel

requirements = [
    "requests",
    "pyOpenSSL",
    "PyJWT",
    "backoff",
    "pytest"
]

setup(
    name='zvi-admin',
    version="1.0.0",
    description='Zorroa Visual Intelligence Admin Client',
    url='http://www.zorroa.com',
    license='Apache2',
    package_dir={'': 'pylib'},
    packages=['zmlp_admin', 'zmlp_admin.app', 'zmlp_admin.entity'],
    scripts=[],
    author="Matthew Chambers",
    author_email="support@zorroa.com",
    keywords="machine learning artificial intelligence",
    python_requires='>=3.4',

    classifiers=[
        "Programming Language :: Python :: 3",
        "Operating System :: OS Independent"
    ],

    include_package_data=True,
    install_requires=requirements
)