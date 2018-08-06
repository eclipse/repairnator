# Testing `check_branches.sh`

The tests on `check_branches.sh` are in the file `check_branches_test.sh`, which relies on the testing framework [shUnit2](https://github.com/kward/shunit2).
To run the tests, two steps are required:

1. Install shUnit2:

```bash
$ sudo apt-get install shunit2 # for debian/ubuntu linux
$ brew install shunit2 # for macos
```

2. Run `check_branches_test.sh`:

```bash
$ ./check_branches_test.sh
```