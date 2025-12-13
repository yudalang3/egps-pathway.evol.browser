destination=/mnt/c/Users/yudal/Documents/project/eGPS2/jars/egps-million_evoltree/dependency-egps/egps-pathway.browser-0.0.1.jar

echo "Start to jar"
jar -cf $destination \
    -C ./out/production/egps-pathway.evol.browser .
 

#echo "cp to 1"
cp $destination /mnt/c//Users/yudal/Documents/project/eGPS2/eGPS_v2.1_windows_64bit/dependency-egps/
cp $destination /mnt/c//Users/yudal/Documents/project/eGPS2/eGPS_v2.1_windows_64bit_selfTest/dependency-egps/
