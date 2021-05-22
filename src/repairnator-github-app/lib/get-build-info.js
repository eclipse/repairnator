const request = require('request-promise-native')

/**
 * Retrieves useful information from the Travis /build API
 * @module lib/get-build-info
 * @param {string} buildUrl - A Travis URL provided by the GitHub API
 * @param {boolean} isOrg - Identify whether it is for travis-ci.org
 */
module.exports = (buildUrl, isOrg) => {
  // Extract the build ID from the target_url provided by GitHub
  const buildId = buildUrl.split('/builds/')[1]
  // Form a URL for the request
  const requestUrl = `https://api.travis-ci.` + (isOrg ? `org` : `com`) + `/v3/build/${buildId}`
  /* alternative request urls
  https://api.travis-ci.org/owner/john/repos
  https://api.travis-ci.org/repo/john%2FfailingProject/builds
  https://api.travis-ci.org/build/637026066
  */

  // Set the parameters for the request to the Travis API
  // https://developer.travis-ci.com/gettingstarted
  const options = {
    uri: requestUrl,
    headers: {
      'Travis-API-Version': 3,
      Authorization: 'token ' + (isOrg ? process.env.TRAVIS_ORG : process.env.TRAVIS_COM)
    },
    json: true
  }

  // Make the request to the Travis API
  // On success return an object with useful info
  return request(options)
    .then(function (build) {
      return {
        pull: build.pull_request_number,
        id: buildId,
        url: requestUrl,
        state: build.state,
        jobs: build.jobs
      }
    })
    .catch(function (err) {
      throw new Error(`Error requesting Travis API ${err}`)
    })
}
