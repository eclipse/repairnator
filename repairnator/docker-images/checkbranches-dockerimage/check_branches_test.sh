#!/usr/bin/env bash

setUp()
{
    REPOSITORY="https://github.com/fermadeiral/test-check-branches"
}

tearDown()
{
    rm -rf repo
}

testWithBranchOk()
{
    BRANCH_NAME="surli-failingProject-208897371-20170308-060702"
    EXPECTED_RESULT="$BRANCH_NAME [OK]"

    RESULT=$(2>&1 $(dirname "${BASH_SOURCE[0]}")/check_branches.sh $REPOSITORY $BRANCH_NAME)

    echo "$RESULT"

    echo "$RESULT" | grep -qF "$EXPECTED_RESULT"
    RETURN_CODE=$?

    assertEquals "The expected result is [OK]" 0 $RETURN_CODE
}

testWithBranchWhereJsonFileDoesNotExist()
{
    BRANCH_NAME="surli-failingProject-208897371-20170308-060702_no_json_file"
    EXPECTED_RESULT="$BRANCH_NAME [FAILURE] (repairnator.json does not exist)"

    RESULT=$(2>&1 $(dirname "${BASH_SOURCE[0]}")/check_branches.sh $REPOSITORY $BRANCH_NAME)

    echo "$RESULT"

    echo "$RESULT" | grep -qF "$EXPECTED_RESULT"
    RETURN_CODE=$?

    assertEquals "The expected result is [FAILURE] (repairnator.json does not exist)" 0 $RETURN_CODE
}

testWithBranchWhereJsonFileIsNotValid()
{
    BRANCH_NAME="surli-failingProject-208897371-20170308-060702_json_file_invalid"
    EXPECTED_RESULT="$BRANCH_NAME [FAILURE] (repairnator.json is invalid)"

    RESULT=$(2>&1 $(dirname "${BASH_SOURCE[0]}")/check_branches.sh $REPOSITORY $BRANCH_NAME)

    echo "$RESULT"

    echo "$RESULT" | grep -qF "$EXPECTED_RESULT"
    RETURN_CODE=$?

    assertEquals "The expected result is [FAILURE] (repairnator.json is invalid)" 0 $RETURN_CODE
}

. shunit2