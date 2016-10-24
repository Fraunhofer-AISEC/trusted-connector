wmic process where (commandline like "%%tpm2d.py%%" and not name="wimc.exe") delete
