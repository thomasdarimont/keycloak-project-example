@echo off

if "%DOMAIN%"=="" (
 DOMAIN=acme.test
)

echo "Generating TLS Certificate and Key for domain: %DOMAIN%"

pushd .
cd .\config\stage\dev\tls
del /F *.pem
mkcert -install %DOMAIN% "*.%DOMAIN%"
echo "TLS Certificate and Key created in %CD%"
dir /B *.pem

popd