#!/bin/sh

/*/ __kotlin_script_installer__ 2>&-
# vim: syntax=kotlin
#    _         _   _ _                       _       _
#   | |       | | | (_)                     (_)     | |
#   | | _____ | |_| |_ _ __    ___  ___ _ __ _ _ __ | |_
#   | |/ / _ \| __| | | '_ \  / __|/ __| '__| | '_ \| __|
#   |   < (_) | |_| | | | | | \__ \ (__| |  | | |_) | |_
#   |_|\_\___/ \__|_|_|_| |_| |___/\___|_|  |_| .__/ \__|
#                         ______              | |
#                        |______|             |_|
v=2.2.0.28
p=org/cikit/kotlin_script/"$v"/kotlin_script-"$v".sh
url="${M2_CENTRAL_REPO:=https://repo1.maven.org/maven2}"/"$p"
kotlin_script_sh="${M2_LOCAL_REPO:-"$HOME"/.m2/repository}"/"$p"
if ! [ -r "$kotlin_script_sh" ]; then
  kotlin_script_sh="$(mktemp)" || exit 1
  fetch_cmd="$(command -v curl) -kfLSso" || \
    fetch_cmd="$(command -v fetch) --no-verify-peer -aAqo" || \
    fetch_cmd="wget --no-check-certificate -qO"
  if ! $fetch_cmd "$kotlin_script_sh" "$url"; then
    echo "failed to fetch kotlin_script.sh from $url" >&2
    rm -f "$kotlin_script_sh"; exit 1
  fi
  dgst_cmd="$(command -v openssl) dgst -sha256 -r" || dgst_cmd=sha256sum
  case "$($dgst_cmd < "$kotlin_script_sh")" in
  "2c927f37e5c2de35b0e9d3d898d957958b27ca62b07bd2da6b009ccfbe9c0631 "*) ;;
  *) echo "error: failed to verify kotlin_script.sh" >&2
     rm -f "$kotlin_script_sh"; exit 1;;
  esac
fi
. "$kotlin_script_sh"; exit 2
*/

import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.*

private data class Item(val src: Path, val targetName: String = src.name) {
    constructor(
        src: String,
        targetName: String = Path(src).name
    ) : this(Path(src), targetName)

    private fun digest(tgt: Path, alg: String) {
        if (tgt.exists() &&
            tgt.getLastModifiedTime() > src.getLastModifiedTime()) return
        val hex = src.inputStream().use { `in` ->
            val md = MessageDigest.getInstance(alg)
            md.update(`in`.readBytes())
            md.digest().joinToString("") {
                String.format("%02x", it)
            }
        }
        tgt.writeText("$hex\n")
    }

    private fun sign(tgt: Path) {
        if (tgt.exists() &&
            tgt.getLastModifiedTime() > src.getLastModifiedTime()) return
        try {
            tgt.deleteIfExists()
        } catch (_: java.nio.file.NoSuchFileException) {
        }
        val rc = ProcessBuilder()
                .command("gpg", "--detach-sign", "--armor", src.pathString)
                .inheritIO()
                .start()
                .waitFor()
        if (rc != 0) error("gpg2 terminated with exit code $rc")
    }

    fun addToZip(zip: ZipOutputStream, tox: String) {
        val md5 = Path("${src.pathString}.md5")
        digest(md5, "MD5")
        val sha1 = Path("${src.pathString}.sha1")
        digest(sha1, "SHA-1")
        val asc = Path("${src.pathString}.asc")
        sign(asc)
        for (f in listOf(
                src to targetName,
                md5 to "$targetName.md5",
                sha1 to "$targetName.sha1",
                asc to "$targetName.asc"
        )) {
            val name = "$tox/${f.second}"
            println("ADD $name")
            val e = ZipEntry(name)
            e.size = f.first.fileSize()
            zip.putNextEntry(e)
            f.first.inputStream().use { `in` ->
                `in`.copyTo(zip)
            }
            zip.closeEntry()
        }
    }
}

fun main(args: Array<String>) {
    val v = args.singleOrNull()?.removePrefix("-v")
            ?: error("usage: publish.kt -v<version>")

    val localRepo = Path(System.getProperty("user.home"), ".m2", "repository")
    val name = "forte"

    ZipOutputStream(Path("publish.zip").outputStream()).use { zip ->
        for (subDir in listOf(name, "$name-js", "$name-jvm")) {
            val subPath = Path("org", "cikit", subDir, v)
            val libDir = localRepo / subPath
            val javadocJar = libDir / "$subDir-$v-javadoc.jar"
            if (subDir.endsWith("-jvm")) {
                ZipOutputStream(javadocJar.outputStream())
                    .use { zip ->
                        zip.putNextEntry(ZipEntry("META-INF/"))
                        zip.closeEntry()
                        zip.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
                        zip.write("Manifest-Version: 1.0\n\n".encodeToByteArray())
                        zip.closeEntry()
                        zip.close()
                    }
            }
            val filesToPublish = listOf(
                Item(libDir / "$subDir-$v-kotlin-tooling-metadata.json"),
                Item(libDir / "$subDir-$v.module"),
                Item(libDir / "$subDir-$v.pom"),
                Item(libDir / "$subDir-$v.jar"),
                Item(libDir / "$subDir-$v.klib"),
                Item(libDir / "$subDir-$v-sources.jar"),
                Item(javadocJar),
            ).filter { item -> item.src.exists() }

            for (item in filesToPublish) {
                item.addToZip(zip, subPath.invariantSeparatorsPathString)
            }
        }
    }
}
