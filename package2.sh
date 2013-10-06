#!/bin/sh

echo "cleaning..."
rm -f *.jar
rm -f *.zip
echo "done"

echo "packaging..."

jar cmf manifest.txt Lasertag3D.jar com icon.gif data rooms default_settings.txt rooms_index.txt

#permissions
chmod +x ./Lasertag3D.jar

echo "done"
exit 0
