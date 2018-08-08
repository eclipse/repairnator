#!/usr/bin/env bash

setUp()
{
    REPOSITORY="https://github.com/fermadeiral/test-check-branches"
}

tearDown()
{
    rm -rf repo
}

testWithBranchFailingPassingOk()
{
    BRANCH_NAME="fermadeiral-test-repairnator-bears-386269112-386271668"
    EXPECTED_RESULT="$BRANCH_NAME [OK]"

    RESULT=$(2>&1 $(dirname "${BASH_SOURCE[0]}")/check_branches.sh $REPOSITORY $BRANCH_NAME)

    echo "$RESULT"

    echo "$RESULT" | grep -qF "$EXPECTED_RESULT"
    RETURN_CODE=$?

    assertEquals "The expected result is [OK]" 0 $RETURN_CODE
}

testWithBranchPassingPassingOk()
{
    BRANCH_NAME="fermadeiral-test-repairnator-bears-386337343-386348522"
    EXPECTED_RESULT="$BRANCH_NAME [OK]"

    RESULT=$(2>&1 $(dirname "${BASH_SOURCE[0]}")/check_branches.sh $REPOSITORY $BRANCH_NAME)

    echo "$RESULT"

    echo "$RESULT" | grep -qF "$EXPECTED_RESULT"
    RETURN_CODE=$?

    assertEquals "The expected result is [OK]" 0 $RETURN_CODE
}

testWithBranchWhereJsonFileDoesNotExist()
{
    BRANCH_NAME="fermadeiral-test-repairnator-bears-386269112-386271668_no_json_file"
    EXPECTED_RESULT="$BRANCH_NAME [FAILURE] (bears.json does not exist)"

    RESULT=$(2>&1 $(dirname "${BASH_SOURCE[0]}")/check_branches.sh $REPOSITORY $BRANCH_NAME)

    echo "$RESULT"

    echo "$RESULT" | grep -qF "$EXPECTED_RESULT"
    RETURN_CODE=$?

    assertEquals "The expected result is [FAILURE] (bears.json does not exist)" 0 $RETURN_CODE
}

testWithBranchWhereJsonFileIsNotValid()
{
    BRANCH_NAME="fermadeiral-test-repairnator-bears-386269112-386271668_json_file_invalid"
    EXPECTED_RESULT="$BRANCH_NAME [FAILURE] (bears.json is invalid)"

    RESULT=$(2>&1 $(dirname "${BASH_SOURCE[0]}")/check_branches.sh $REPOSITORY $BRANCH_NAME)

    echo "$RESULT"

    echo "$RESULT" | grep -qF "$EXPECTED_RESULT"
    RETURN_CODE=$?

    assertEquals "The expected result is [FAILURE] (bears.json is invalid)" 0 $RETURN_CODE
}

. shunit2