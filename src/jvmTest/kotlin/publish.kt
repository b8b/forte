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
v=1.9.21.21
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
  "72fdbaf8238e40303a9115b2c3177c6717a928b60520ccdde7e42b265164f494 "*) ;;
  *) echo "error: failed to verify kotlin_script.sh" >&2
     rm -f "$kotlin_script_sh"; exit 1;;
  esac
fi
. "$kotlin_script_sh"; exit 2
*/

import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.*

private data class Item(val src: Path, val targetName: String = src.name) {
    private fun digest(tgt: Path, alg: String) {
        if (tgt.exists() &&
            tgt.getLastModifiedTime() > src.getLastModifiedTime()) {
            return
        }
        val hex = src.inputStream().use { `in` ->
            val md = MessageDigest.getInstance(alg)
            val buffer = ByteArray(1024 * 4)
            while (true) {
                val read = `in`.read(buffer)
                if (read < 0) {
                    break
                }
                md.update(buffer, 0, read)
            }
            md.digest().joinToString("") {
                String.format("%02x", it)
            }
        }
        tgt.writeText("$hex\n")
    }

    private fun sign(tgt: Path) {
        if (tgt.exists() &&
            tgt.getLastModifiedTime() > src.getLastModifiedTime()) {
            return
        }
        try {
            tgt.deleteExisting()
        } catch (_: java.nio.file.NoSuchFileException) {
        }
        val rc = ProcessBuilder()
            .command("gpg", "--detach-sign", "--armor", src.toString())
            .inheritIO()
            .start()
            .waitFor()
        if (rc != 0) error("gpg2 terminated with exit code $rc")
    }

    fun upload(to: String, base64Credentials: String) {
        val md5 = Path("$src.md5")
        digest(md5, "MD5")
        val sha1 = Path("$src.sha1")
        digest(sha1, "SHA-1")
        val asc = Path("$src.asc")
        sign(asc)
        for ((f, name) in listOf(
            src to targetName,
            md5 to "$targetName.md5",
            sha1 to "$targetName.sha1",
            asc to "$targetName.asc"
        )) {
            val url = URL("$to/$name")
            println("PUT $url")
            val cn = url.openConnection() as HttpURLConnection
            cn.requestMethod = "PUT"
            cn.doOutput = true
            cn.addRequestProperty("Authorization", "Basic $base64Credentials")
            cn.outputStream.use { out ->
                f.inputStream().use { `in` -> `in`.copyTo(out) }
            }
            val responseBody = cn.inputStream.use { it.readBytes() }
            if (!cn.responseCode.toString().startsWith("2")) {
                error("failed with status ${cn.responseCode} " +
                        "${cn.responseMessage}\n" +
                        responseBody.toString(Charsets.UTF_8))
            }
        }
    }
}

fun main(args: Array<String>) {
    val v = args.singleOrNull()?.removePrefix("-v")
        ?: error("usage: publish.kt -v<version>")
    val user = System.console().readLine("Username: ")
    val pass = System.console().readPassword("Password: ")
    val base64Credentials = Base64.getUrlEncoder().encodeToString(
        "$user:${String(pass)}".toByteArray()
    )

    val repo = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    val localRepo = Path(System.getProperty("user.home"), ".m2", "repository")
    val name = "forte"
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
            item.upload(
                "$repo/${subPath.invariantSeparatorsPathString}",
                base64Credentials
            )
        }
    }
}
