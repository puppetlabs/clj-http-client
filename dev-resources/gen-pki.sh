#!/bin/bash

if ! [[ -d dev-resources/ssl ]]; then
  echo "This script must be called from the root of the project and dev-resources/ssl must already exist"
  exit 1
fi

echo
echo "Generating primary self-signed CA"
openssl req -x509 \
  -newkey rsa:4096 \
  -keyout dev-resources/ssl/ca.key \
  -out dev-resources/ssl/ca.pem \
  -days 1825 -nodes \
  -extensions x509v3_CA \
  -config dev-resources/exts.cnf \
  -subj "/C=US/ST=OR/L=Portland/O=Puppet, Inc/CN=puppet"

echo
echo "Generating node cert"
openssl genrsa -out dev-resources/ssl/key.pem 2048

echo
echo "Creating node CSR"
openssl req -new -sha256 \
  -key dev-resources/ssl/key.pem \
  -out dev-resources/ssl/csr.pem \
  -subj "/C=US/ST=OR/L=Portland/O=Puppet, Inc/CN=localhost"

echo
echo "Signing node CSR"
openssl x509 -req \
  -in dev-resources/ssl/csr.pem \
  -CA dev-resources/ssl/ca.pem \
  -CAkey dev-resources/ssl/ca.key \
  -CAcreateserial \
  -out dev-resources/ssl/cert.pem \
  -days 1825 -sha256

echo
echo "Generating alternate self-signed CA"
openssl req -x509 \
  -newkey rsa:4096 \
  -keyout dev-resources/ssl/alternate-ca.key \
  -out dev-resources/ssl/alternate-ca.pem \
  -days 1825 -nodes \
  -extensions x509v3_CA \
  -config dev-resources/exts.cnf \
  -subj "/C=US/ST=OR/L=Portland/O=Puppet, Inc/CN=alternate"


echo
echo "Cleaning up files that will not be used by the tests"
rm dev-resources/ssl/{alternate-ca.key,ca.key,ca.srl,csr.pem}
