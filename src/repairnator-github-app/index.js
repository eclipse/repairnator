const touchMQ = require('./lib/touch-mq')
const getBuildInfo = require('./lib/get-build-info')
const getLog = require('./lib/get-log')
const extractLog = require('./lib/extract-log')
const makeComment = require('./lib/make-comment')

/**
 * This is the main entrypoint to your Probot app
 * @param {import('probot').Application} app
 */
module.exports = app => {
  app.log('Yay, app loaded!')

  app.on(`*`, async context => {
    context.log({ event: context.event, action: context.payload.action })
  })

  // app.on('issues.opened', async context => {
  //   app.log('issues.opened...')
  //   const issueComment = context.issue({ body: 'Thanks for opening this issue!' })
  //   return context.github.issues.createComment(issueComment)
  // })

  app.on('check_run.completed', async context => {
    context.log('Check run...');
    context.log({ conclusion: context.payload.check_run.conclusion})
    if(
        context.payload.check_run.name == "Travis CI - Pull Request" && 
        context.payload.check_run.conclusion == "failure"
      )  {
        const isOrg = context.payload.check_run.details_url.includes('travis-ci.org')

        // get the build info
        const buildInfo = await getBuildInfo(context.payload.check_run.details_url, isOrg)

        // Get the log info
        const log = await getLog(buildInfo.jobs, isOrg)

        // Extract the relevant info & clean up the log
        const logInfo = extractLog(log)

        // Form a comment by passing the error message, Travis build URL, commit sha, repo name, and PR#
        const comment = makeComment(
          context.payload.check_run.details_url,
          context.payload.check_run.head_sha,
          context.payload.repository.full_name,
          buildInfo.pull,
          buildInfo.id,
          buildInfo.state,
          logInfo.language
        )
        
        if (buildInfo.state === 'failed' && logInfo.language.split('\r\n')[0] === 'java') {          
          touchMQ(buildInfo.id, isOrg);
          // Post a comment to the Pull Request
          const params = context.issue({
            body: comment,
            issue_number: buildInfo.pull
          })
          return context.github.issues.createComment(params)
        }  
      }

  });

  app.on('status', async context => {
    // Check if the returned context state is a failure
    // && that the failure comes from a Travis Pull Request build
    context.log('status...')
    context.log({ state: context.payload.state, context: context.payload.context })

    if (
      context.payload.state === 'failure' &&
      context.payload.context === 'continuous-integration/travis-ci/pr'
    ) {
      // Check it is travis-ci.org or travis-ci.com
      const isOrg = context.payload.target_url.includes('travis-ci.org')

      // get the build info
      const buildInfo = await getBuildInfo(context.payload.target_url, isOrg)

      // Get the log info
      const log = await getLog(buildInfo.jobs, isOrg)

      // Extract the relevant info & clean up the log
      const logInfo = extractLog(log)

      // Form a comment by passing the error message, Travis build URL, commit sha, repo name, and PR#
      const comment = makeComment(
        context.payload.target_url,
        context.payload.sha,
        context.payload.name,
        buildInfo.pull,
        buildInfo.id,
        buildInfo.state,
        logInfo.language
      )

      if (buildInfo.state === 'failed' && logInfo.language === 'java') {
        touchMQ(buildInfo.id, isOrg);
        // Post a comment to the Pull Request
        const params = context.issue({
          body: comment,
          number: buildInfo.pull
        })
        return context.github.issues.createComment(params)
      }
    }
  })

  // For more information on building apps:
  // https://probot.github.io/docs/

  // To get your app running against GitHub, see:
  // https://probot.github.io/docs/development/
}