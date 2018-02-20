import Scanner from '../models/scanner.model';

/**
 * Load user and append to req.
 */
function load(req, res, next, id) {
  Scanner.get(id)
    .then((scanner) => {
      req.scanner = scanner; // eslint-disable-line no-param-reassign
      return next();
    })
    .catch(e => next(e));
}

/**
 * Get user
 * @returns {User}
 */
function get(req, res) {
  return res.json(req.scanner);
}

/**
 * Get user list.
 * @property {number} req.query.skip - Number of users to be skipped.
 * @property {number} req.query.limit - Limit number of users to be returned.
 * @returns {User[]}
 */
function list(req, res, next) {
  Scanner.list()
    .then(result => res.json(result))
    .catch(e => next(e));
}

function count(req, res, next) {
  Scanner.count()
    .then(result => res.json(result))
    .catch(e => next(e));
}

function monthData(req, res, next) {
  Scanner.getLastMonthData().then(result => res.json(result)).catch(e => next(e));
}

function weeksData(req, res, next) {
  Scanner.getLastWeeksData(req.nbWeeks).then(result => res.json(result)).catch(e => next(e));
}

export default { load, get, list, count, monthData, weeksData };
