import moment from 'moment';
import _ from 'lodash';

import RtScanner from '../models/rtscanner.model';

/**
 * Load rtScanner and append to req.
 */
function load(req, res, next, id) {
  RtScanner.get(id)
    .then((rtScanner) => {
      req.rtScanner = rtScanner; // eslint-disable-line no-param-reassign
      return next();
    })
    .catch(e => next(e));
}

/**
 * Get rtScanner
 * @returns {RtScanner}
 */
function get(req, res) {
  return res.json(req.rtScanner);
}

/**
 * Get rtScanner list.
 * @property {number} req.query.skip - Number of users to be skipped.
 * @property {number} req.query.limit - Limit number of users to be returned.
 * @returns {RtScanner[]}
 */
function list(req, res, next) {
  const { limit = 50, skip = 0 } = req.query;
  RtScanner.list({ limit, skip })
    .then(result => res.json(result))
    .catch(e => next(e));
}


function speedrate(req, res, next) {
  RtScanner.speedrate()
    .then((result) => {
      const gtDateIso = moment().startOf('hour').subtract(24, 'hours');
      const baseStatus = {
        ERRORED: 0,
        FAILED: 0,
        PASSED: 0,
        CANCELED: 0,
      };
      // Array with count of 0
      const hours = _.map(_.range(24), (plusHour) => {
        const time = gtDateIso.clone().add(plusHour, 'hours');
        return {
          _id: time,
          status: baseStatus,
        };
      });
      // String to moment
      const parsedValues = result.map((value) => {
        // The query drop the timezone then we have to re-insert it
        const time = moment(`${value._id}:00:00.000+00:00`);
        const status = _.reduce(value.status, (acc, cur) => {
          acc[cur] = acc[cur] + 1 || 1; // eslint-disable-line no-param-reassign
          return acc;
        }, _.clone(baseStatus));
        return { _id: time, status };
      });

      return res.json(_.sortBy(_.unionWith(parsedValues, hours, (a, b) => a._id.isSame(b._id)), '_id'));
    })
    .catch(e => next(e));
}

export default {
  load,
  get,
  list,
  speedrate,
};
