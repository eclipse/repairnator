import Inspector from '../models/inspector.model';

/**
 * Load user and append to req.
 */
function load(req, res, next, id) {
  Inspector.get(id)
    .then((inspector) => {
      req.inspector = inspector; // eslint-disable-line no-param-reassign
      return next();
    })
    .catch(e => next(e));
}

function get(req, res) {
  return res.json(req.inspector);
}

/**
 * Get user list.
 * @property {number} req.query.skip - Number of users to be skipped.
 * @property {number} req.query.limit - Limit number of users to be returned.
 * @returns {User[]}
 */
function list(req, res, next) {
  const { limit = 50, skip = 0 } = req.query;
  Inspector.list({ limit, skip })
    .then(users => res.json(users))
    .catch(e => next(e));
}

function count(req, res, next) {
  Inspector.count()
    .then(result => res.json(result))
    .catch(e => next(e));
}

function hostnameStats(req, res, next) {
  Inspector.hostnameStats()
    .then(result => res.json(result))
    .catch(e => next(e));
}

function statusStats(req, res, next) {
  Inspector.statusStats(0)
    .then(result => res.json(result))
    .catch(e => next(e));
}

function statusStatsPeriod(req, res, next) {
  Inspector.statusStats(req.nbDays)
    .then(result => res.json(result))
    .catch(e => next(e));
}

function nbUniqueBuilds(req, res, next) {
  Inspector.nbUniqueBuilds()
    .then(result => res.json(result))
    .catch(e => next(e));
}

function reproducedBuildsAll(req, res, next) {
  Inspector.reproducedErrors(0)
    .then(result => res.json(result))
    .catch(e => next(e));
}

function reproducedBuilds(req, res, next) {
  Inspector.reproducedErrors(req.nbDays)
    .then(result => res.json(result))
    .catch(e => next(e));
}

function getPatches(req, res, next) {
  Inspector.getPatches()
    .then(result => res.json(result))
    .catch(e => next(e));
}

function getNbFailuresByProject(req, res, next) {
  Inspector.nbFailuresByProject()
    .then(result => res.json(result))
    .catch(e => next(e));
}

function getNbReproducedByProject(req, res, next) {
  Inspector.nbReproducedByProject()
    .then(result => res.json(result))
    .catch(e => next(e));
}

function countSuccessFullyReproducedBuild(req, res, next) {
  Inspector.countReproducedErrors()
    .then(result => res.json(result))
    .catch(e => next(e));
}

export default { load, get, list, count, hostnameStats, statusStats, nbUniqueBuilds, statusStatsPeriod, reproducedBuilds, reproducedBuildsAll, getPatches, getNbFailuresByProject, getNbReproducedByProject, countSuccessFullyReproducedBuild };
