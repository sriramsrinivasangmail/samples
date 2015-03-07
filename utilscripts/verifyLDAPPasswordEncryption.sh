#!/bin/sh
#################################################################
# This script verifies existing passwords are encrypted in SSHA #
#################################################################

scriptHome="/opt/ibm/dsserver/scripts"
dswebserverProperties="$scriptHome/../Config/dswebserver.properties"

TRUE=1
FALSE=0
SUCCESS=0
FAILURE=1

connected=$FALSE
rootDn="cn=bluldap,dc=blustratus,dc=com"
rootPassword=""
unencrypted=0

sourceDB2Profile() {
    type -P db2 &>/dev/null || . ~/sqllib/db2profile
}

checkPasswordEncryption() {
    local user=$1
    local password
    password=$(sudo slapcat -a "uid=$user" 2>&1 | grep -i userPassword | awk "{print \$2}")
    local encrypted=$(python -c "import base64; print base64.urlsafe_b64decode(\"$password\")" | grep \{SSHA\})
    if [ -z "$encrypted" ]; then
        echo "$user: not Encrypted"
        let unencrypted=$unencrypted+1
    else
        echo "$user: Encrypted"
    fi
}

ldapUserExists() {
    local user=$1
    output=$(ldapsearch -x "uid=$user" | grep -i "uid=$user,ou=People,dc=blustratus,dc=com")
    if [ -n "$output" ]; then
        return $TRUE
    fi
    return $FALSE
}

checkAllUsersPasswords() {
    connectOutput=$(db2 -x CONNECT TO BLUDB)
    connected=$TRUE
    tablespaces=$(db2 -x "SELECT TBSPACE FROM SYSCAT.TABLESPACES WHERE TBSPACE LIKE '%space1'")
    if [ $? -eq $SUCCESS ]; then
        tablespacesQuerySuccess=$SUCCESS
    else
        tablespacesQuerySuccess=$FAILURE
    fi
    resetOutput=$(db2 -x CONNECT RESET)
    connected=$FALSE

    if [ $tablespacesQuerySuccess -eq $SUCCESS ] ; then
        while read -r tablespace; do
            user=$(echo $tablespace | sed 's/space1//g')
            ldapUserExists "$user" 
            if [ $? -eq $TRUE ]; then
                checkPasswordEncryption "$user"
            else
                echo "No LDAP entry for $user, skipping"
            fi
        done <<< "$tablespaces"
        echo "Number of unencrypted passwords: $unencrypted"
    else
        echo "Error: could not retrieve all tablespace names"
    fi
}

sourceDB2Profile
checkAllUsersPasswords
