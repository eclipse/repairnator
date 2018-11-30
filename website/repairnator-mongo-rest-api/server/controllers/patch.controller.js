import Patch from '../models/patch.model';

/**
 * Load patch and append to req.
 */
function load(req, res, next, id) {
  Patch.get(id)
    .then((patch) => {
      req.patch = patch; // eslint-disable-line no-param-reassign
      return next();
    })
    .catch(e => next(e));
}

/**
 * Get patch
 * @returns {Patch}
 */
function get(req, res) {
  return res.json(req.patch);
}

/**
 * Get patch list.
 * @property {number} req.query.skip - Number of patches to be skipped.
 * @property {number} req.query.limit - Limit number of patches to be returned.
 * @returns {RtScanner[]}
 */
function list(req, res, next) {
  const { limit = 50, skip = 0 } = req.query;
  Patch.list({ limit, skip })
    .then(result => res.json(result))
    .catch(e => next(e));
}

function listBuildPatches(req, res, next) {
  Patch.listBuildPatches(req.params.buildId)
    .then(result => res.json(result))
    .catch(e => next(e));
}

export default {
  load,
  get,
  list,
  listBuildPatches,
};
