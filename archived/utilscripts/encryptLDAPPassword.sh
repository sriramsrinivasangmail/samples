#!/bin/sh
##################################################################
# This script updates existing passwords to be encrypted in SSHA #
##################################################################

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

#arg1 is propfile, arg2 is key
#returns empty if not found, else the value for that key
getpropfrompropfile() {
    sort ${1} | uniq | grep  "^${2}=" | sed "s%${2}=\(.*\)%\1%"
}

#arg1 is key in dswebserver.properties
#returns empty if not found, else the value for that key
getdswebserverprop() {
    getpropfrompropfile ${dswebserverProperties} ${1}
}

encryptPassword() {
    local user=$1
    local password
    password=$(ldapsearch -x "uid=$user" | grep -i userPassword | awk "{print \$2}")
    local encrypted=$(python -c "import base64; print base64.urlsafe_b64decode(\"$password\")" | grep \{SSHA\})
    if [ -z "$encrypted" ]; then
        echo "$user: not encrypted ... encrypting"
        let unencrypted=$unencrypted+1
        local encryptedPassword=$(slappasswd -h {SSHA} -s $password)
        ldapmodify=$(ldapmodify -x -v -D "$rootDn" -w "$rootPassword" <<HASH_PASSWORD 2>&1
dn: uid=$user,ou=People,dc=blustratus,dc=com
changetype: modify
replace: userPassword
userPassword: $encryptedPassword
HASH_PASSWORD
    )
    else
        echo "$user: encrypted, skipping"
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

encryptAllUsersPasswords() {
    connectOutput=$(db2 -x CONNECT TO BLUDB)
    connected=$TRUE
    tablespaces=$(db2 -x "SELECT TBSPACE FROM SYSCAT.TABLESPACES WHERE TBSPACE LIKE '%space1'")
    if [ $? -eq 0 ]; then
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
                encryptPassword "$user"
            else
                echo "No LDAP entry for $user, skipping"
            fi
        done <<< "$tablespaces"
        echo "Number of unencrypted passwords: $unencrypted"
    else
        echo "Error: could not retrieve all tablespace names"
    fi
}

rootPassword=$(getdswebserverprop "ldap.root.passwd")
rootPassword=$($scriptHome/decrypt.sh $rootPassword)

encryptAllUsersPasswords
