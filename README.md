# Howto

Create a fork of the repository and clone it if not done yet.

## Create key & certificate signing request
```
export MY_NAME="Your name"
openssl req -out "${MY_NAME}.csr" -new -newkey rsa:2048 -nodes -keyout "${MY_NAME}.key" -subj '/'
```

## Commit and create pull request
```
git checkout -b "csr/${MY_NAME}" dev-certs
git add "${MY_NAME}.csr"
git commit -m "CSR for '${MY_NAME}'"
git push
```

Don't commit your key.
Create a pull request on Github for your forked branch.

## Create the final certificate/key bundle
Once the PR is merged fetch the generated certificate.
```
git checkout dev-certs
git pull
openssl pkcs12 -export -in "${MY_NAME}.crt" -inkey "${MY_NAME}.key" -out "${MY_NAME}.p12" -passout pass:
```

Use the p12 file as described [here](https://github.com/warmshowers/wsandroid#use-the-development-proxy-server).

## Please note

The development proxy server accesses the Warmshowers development site at
https://dev.warmshowers.org.

## Important
You are not allowed to distribute any app that uses the development proxy server!
Use it to contribute to the [wsandroid project](https://github.com/warmshowers/wsandroid) only.
