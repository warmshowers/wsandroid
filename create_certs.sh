#!/bin/bash
# Creates certificates for all .csr files in the main directory.
# It then adds them to git and removes the csr files.

echo "$WS_DEV_CA_KEY" > CA/ws-dev-CA.key
generatedNewCert=false
for csr in *.csr; do
    [ -f "$csr" ] || break

    echo "Processing '$csr'"
    crt=${csr:0:-4}.crt
    if [ -f "$crt" ]; then
        # This cert already exists.
        echo "Skipping '$csr' as a certificate already exists"
        continue;
    fi

    echo "Creating certificate '$crt'"
    generatedNewCert=true
    openssl x509 -req \
        -CA CA/ws-dev-CA.crt -CAkey CA/ws-dev-CA.key -CAserial CA/ws-dev-CA.srl \
        -in "$csr" -out "$crt" \
        -days 3650 \
        2>&1 > /dev/null
    git add "$crt"
    git rm "$csr"
done
rm -f CA/ws-dev-CA.key

if ! $generatedNewCert; then
    # Nothing changed.
    echo "Nothing changed"
    exit 0
fi

# Add the serial number file.
git add CA/ws-dev-CA.srl

git -c "user.name=WS Cert Robot" -c "user.email=noreply@warmshowers.org" \
    commit -m "Sign certificates"
