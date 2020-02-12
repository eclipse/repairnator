const stripAnsi = require('strip-ansi')

/**
 * Extracts the error message from a raw Travis log
 * @module lib/extract-log
 * @param {string} log - The raw Travis log
 * @return {string} - The clean error message
 */
module.exports = log => {
    // Strip the logs of ANSI escape codes
    // https://github.com/chalk/strip-ansi
    const cleanLog = stripAnsi(log)

    const systemInfo = cleanLog
        .split('Build system information')[1]
        .split('Build image provisioning date and time')[0]
        .trim()
    const language = systemInfo
        .split('Build language:')[1]
        .split('Build group:')[0]
        .trim()
    const buildId = systemInfo
        .split('Build id:')[1]
        .split('Job id:')[0]
        .trim()
    const jobId = systemInfo
        .split('Job id:')[1]
        .split('Runtime kernel version:')[0]
        .trim()
    return {
        language: language,
        buildId: buildId,
        jobId: jobId
    }
}