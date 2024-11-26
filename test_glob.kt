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
v=2.0.21.25
p=org/cikit/kotlin_script/"$v"/kotlin_script-"$v".sh
url="${M2_CENTRAL_REPO:=https://nexus.seekda.com/repository/public}"/"$p"
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
  "777b8077ec6a1b9c2da48b056d324deda075cdca522eb504d822c59a2c12af80 "*) ;;
  *) echo "error: failed to verify kotlin_script.sh" >&2
     rm -f "$kotlin_script_sh"; exit 1;;
  esac
fi
. "$kotlin_script_sh"; exit 2
*/

///PLUGIN=org.jetbrains.kotlin:kotlin-serialization-compiler-plugin-embeddable

///DEP=org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.7.3
///DEP=org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.7.3

///DEP=org.cikit:forte-jvm:0.4.0

import org.cikit.forte.core.Glob

//"$text" "$pattern" $match_glob "wildmatch"

fun main(args: Array<String>) {
    val text = args[0]
    val pattern = args[1]
    val expected = args[2].toInt() != 0

    println("---")
    println("input=$text")
    println("pattern=$pattern")
    try {
        val converted = Glob(pattern, Glob.Flavor.Git).toRegex()
        val result = converted.matches(text)
        println("converted: $converted")
        if (result != expected) {
            print("!! ")
        }
        println("result=$result, expected=$expected")
    } catch (ex: Exception) {
        println("exception=$ex")
    }
}
