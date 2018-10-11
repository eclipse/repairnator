import PipelineError from '../models/pipeline-error.model';

/**
 * Load user and append to req.
 */
function load(req, res, next, id) {
  PipelineError.get(id)
    .then((pipelineError) => {
      req.pipelineError = pipelineError; // eslint-disable-line no-param-reassign
      return next();
    })
    .catch(e => next(e));
}

function get(req, res) {
  return res.json(req.pipelineError);
}

/**
 * Get user list.
 * @property {number} req.query.skip - Number of users to be skipped.
 * @property {number} req.query.limit - Limit number of users to be returned.
 * @returns {User[]}
 */
function list(req, res, next) {
  const { limit = 50, skip = 0 } = req.query;
  PipelineError.list({ limit, skip })
    .then(users => res.json(users))
    .catch(e => next(e));
}

export default {
  load,
  get,
  list,
};
