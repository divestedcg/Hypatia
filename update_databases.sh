wget -N https://divested.dev/MalwareScannerSignatures/hypatia-md5-bloom.bin
wget -N https://divested.dev/MalwareScannerSignatures/hypatia-md5-bloom.bin.sig
gpg --verify hypatia-md5-bloom.bin.sig

wget -N https://divested.dev/MalwareScannerSignatures/hypatia-md5-extended-bloom.bin
wget -N https://divested.dev/MalwareScannerSignatures/hypatia-md5-extended-bloom.bin.sig
gpg --verify hypatia-md5-extended-bloom.bin.sig

wget -N https://divested.dev/MalwareScannerSignatures/hypatia-sha1-bloom.bin
wget -N https://divested.dev/MalwareScannerSignatures/hypatia-sha1-bloom.bin.sig
gpg --verify hypatia-sha1-bloom.bin.sig

wget -N https://divested.dev/MalwareScannerSignatures/hypatia-sha256-bloom.bin
wget -N https://divested.dev/MalwareScannerSignatures/hypatia-sha256-bloom.bin.sig
gpg --verify hypatia-sha256-bloom.bin.sig
