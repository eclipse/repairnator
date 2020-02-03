/**
 * Creates a template for the GitHub comment
 * @module lib/comment-template
 * @param {string} errorMsg - The error message extracted from Travis
 * @param {string} travisUrl - The URL of the Travis build
 * @param {string} sha - The sha of the commit on which the build failed
 * @param {string} repoName - The name of the repo, follows user/name format
 * @param {string} pull - The pull request number
 * @return {string} - The error message template
 */
module.exports = (travisUrl, sha, repoName, pull, buildId, buildState, language) => {
  // shorten the sha
  const shortSha = sha.substring(0, 6)
  // create the link to the commit's URL in the context of the PR
  const commitUrl = `https://github.com/${repoName}/pull/${pull}/commits/${sha}`
  const owner = repoName.split('/')[0]
  const repo = repoName.split('/')[1]

  // left outdented to account for template literal whitespace
  return `:wave: Hi, Travis CI [reports](${travisUrl}) a failure for commit [${shortSha}](${commitUrl}). :robot: [Luc Esape](https://github.com/lucesape) will have a look at it soon.

--The [repairnator-bot app](https://github.com/apps/repairnator-bot)
`
}
