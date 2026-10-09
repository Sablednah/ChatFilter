# Shared by the rig scripts. The ChatFilter Bukkit test rig: ~/mc/chatfilter-bukkit on Vivo, one
# folder per server line, server jars copied from MobHealth's rig (~/mc/mobhealth-bukkit).
B=~/mc/chatfilter-bukkit
VERSIONS=(1.7.10 1.8.8 1.12.2 1.16.5 1.20.1 1.21.11 26.1.2 26.3)

# version -> port, claimed in ~/dev/README.md as 25621-25628. RCON is port+100.
port_of() {
  case "$1" in
    1.7.10) echo 25621 ;; 1.8.8) echo 25622 ;; 1.12.2) echo 25623 ;; 1.16.5) echo 25624 ;;
    1.20.1) echo 25625 ;; 1.21.11) echo 25626 ;; 26.1.2) echo 25627 ;; 26.3) echo 25628 ;;
    *) echo "" ;;
  esac
}

# version -> java and jar suffix
java_of() {
  case "$1" in
    26.*)           echo /usr/lib/jvm/java-25-openjdk-amd64/bin/java ;;
    1.21.*)         echo /usr/lib/jvm/java-21-openjdk-amd64/bin/java ;;
    1.20.*|1.17.*|1.18.*|1.19.*) echo "$HOME/dev/jdk17-cityworld/bin/java" ;;
    *)              echo "$HOME/dev/jdk8-mobhealth/bin/java" ;;
  esac
}
suffix_of() {
  case "$1" in
    1.7.*|1.8.*|1.9.*|1.10.*|1.11.*|1.12.*) echo bukkit1.7-1.12 ;;
    *) echo bukkit1.13-26.3 ;;
  esac
}
