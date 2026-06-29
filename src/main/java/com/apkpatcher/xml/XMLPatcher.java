package com.apkpatcher.xml;

import com.apkpatcher.util.Log;

import com.reandroid.apk.ApkModule;
import com.reandroid.apk.xmlencoder.XMLEncodeSource;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ValueType;
import com.reandroid.archive.ByteInputSource;
import com.reandroid.xml.source.XMLStringParserSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class XMLPatcher {

    public static void addNSC(ApkModule apkModule) throws IOException {

        TableBlock table = apkModule.getTableBlock();
        PackageBlock pkg = table.pickOne();

        if (pkg == null) {
            Log.e("[ERROR]", "No package found in TableBlock!");
            return;
        }

        Log.i("[INFO]", "Patching XMLs...");

        /* --- Add Certificate in raw */
        /* The default certificate is from HttpCanary modified by Techno India. */
        String default_certificate =
"""
-----BEGIN CERTIFICATE-----
MIIDczCCAlugAwIBAgIHALdlRG+pDzANBgkqhkiG9w0BAQ0FADBHMRswGQYDVQQD
DBJIdHRwQ2FuYXJ5IFJvb3QgQ0ExEzARBgNVBAoMCkh0dHBDYW5hcnkxEzARBgNV
BAsMCkh0dHBDYW5hcnkwHhcNMjIwMzA2MTIxMTAxWhcNMzMwMzAzMTIxMTAxWjBH
MRswGQYDVQQDDBJIdHRwQ2FuYXJ5IFJvb3QgQ0ExEzARBgNVBAoMCkh0dHBDYW5h
cnkxEzARBgNVBAsMCkh0dHBDYW5hcnkwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw
ggEKAoIBAQCrzm03w7mMvHujpl0IMb/jgxEwJdUsfpazdgUVdsq+7T/Ks8O3NMFP
d4hl6sUgRbaMx3Uz8WolEtz/wu/fdGnrUVDcdWXiJKfhLUUP3KuYwE9tahrfRf14
Yg/xGoA8Pz1BEaUnsJSt6RB5qm5fwn2O8QRykAbgr11or2rr8KQWMaoeciN04tjd
kkcWmPWNSytwea7l1LOrolUXGcbFlpXpGY1cTCoB1RZJe7HkUd1zdYhKUlhHZo3P
in9FhGa/UJGlyWXmT3ybY0nuPtIvqJ3Ao4FwP1zkrrqvS0UCi3QvJZrZ8EEju0U9
NM009njCT6sX56TUG189Dk1uettEiTtlAgMBAAGjZDBiMB0GA1UdDgQWBBT0yJzC
NcHzwIVXMTnvgPp74q1KWjAPBgNVHRMBAf8EBTADAQH/MAsGA1UdDwQEAwIBtjAj
BgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQEN
BQADggEBAA9H0nWzKUKKfgu6RI657wVgSONymRRnpzQ+GNjbDoi6CR3QWL8SvPe8
s61nM8xUP0aMFv0VYrd80sICTQXAEld+/eXoDib7qxg1I2I9v+FkLwPSN2FaJRkv
GKxfki4s6kpNNvmO5X+1eR1fK7Y/lrlp9V7zP8oMbcBuNkiWO6UYNGGGuqxFr3H4
f4LRvODZks/aGea2E0pdiAnAZCIGZS3Mg5cS7wA5vUSkKwpBIcYFVdYTF/xblJfX
OBoyS7CMCG66aSfs3zk4lT8fVwtFJjvkM01gH3A4q6T78rZ/Nkx01GC90Y1+xDAW
0o1SBaeL3tulFzqhMkl5KW0F3vYpP8k=
-----END CERTIFICATE-----""";

        String certPath = "res/raw/Techno_India.pem";
        byte[] certBytes = default_certificate.getBytes(StandardCharsets.UTF_8);

        apkModule.add(new ByteInputSource(certBytes, certPath));

        Entry certEntry = pkg.getOrCreate("", "raw", "Techno_India");
        certEntry.setValueAsString(certPath);
        Log.i("[INFO]", "Added CERT: " + certPath);

        /* --- Add Network Security Config in xml */
        String path = "res/xml/network_security_config.xml";

        String xml =
"""
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">*</domain>
        <domain includeSubdomains="true">0.0.0.0</domain>
        <domain includeSubdomains="true">127.0.0.1</domain>
        <trust-anchors>
            <certificates src="@raw/Techno_India" overridePins="true" />
            <certificates src="system" overridePins="true" />
            <certificates src="user" overridePins="true" />
        </trust-anchors>
    </domain-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="@raw/Techno_India" overridePins="true" />
            <certificates src="system" overridePins="true" />
            <certificates src="user" overridePins="true" />
        </trust-anchors>
    </base-config>
    <debug-overrides>
        <trust-anchors>
            <certificates src="@raw/Techno_India" overridePins="true" />
            <certificates src="system" overridePins="true" />
            <certificates src="user" overridePins="true" />
        </trust-anchors>
    </debug-overrides>
</network-security-config>
""";

        apkModule.add(
            new XMLEncodeSource(
                pkg,
                new XMLStringParserSource(
                    path,
                    xml
                )
            )
        );

        Entry xmlEntry = pkg.getOrCreate("", "xml", "network_security_config");
        xmlEntry.setValueAsString(path);
        Log.i("[INFO]", "Added NSC: " + path);

        /* --- Add attributes in Manifest */
        AndroidManifestBlock manifest = apkModule.getAndroidManifest();
        ResXmlElement app = manifest.getOrCreateApplicationElement();

        app.getOrCreateAndroidAttribute("usesCleartextTraffic", 0x010104ec)
            .setValueAsBoolean(true);
        Log.i("[INFO]", "Added Attribute usesCleartextTraffic");

        app.getOrCreateAndroidAttribute("networkSecurityConfig", 0x01010527)
            .setTypeAndData(ValueType.REFERENCE, xmlEntry.getResourceId());
        Log.i("[INFO]", "Added Attribute networkSecurityConfig");

        manifest.refreshFull();
    }
}