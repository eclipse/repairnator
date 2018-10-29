/* eslint-disable no-trailing-spaces */
import Promise from 'bluebird';
import mongoose from 'mongoose';
import httpStatus from 'http-status';
import moment from 'moment';
import APIError from '../helpers/APIError';

/**
 * Scanner Schema
 */
const RtScannerSchema = new mongoose.Schema({
  hostname: String,
  runId: String,
  dateWatched: Date,
  dateWatchedStr: String,
  dateBuildEnd: Date,
  dateBuildEndStr: String,
  repository: String,
  buildId: Number,
  status: String,
}, { collection: 'rtscanner' });

/**
 * Add your
 * - pre-save hooks
 * - validations
 * - virtuals
 */

/**
 * Methods
 */
RtScannerSchema.method({
});

/**
 * Statics
 */
RtScannerSchema.statics = {
  /**
   * Get inspector
   * @param {ObjectId} id - The objectId of user.
   * @returns {Promise<User, APIError>}
   */
  get(id) {
    return this.findById(id)
      .exec()
      .then((rtScanner) => {
        if (rtScanner) {
          return rtScanner;
        }
        const err = new APIError('No such scanner data exists!', httpStatus.NOT_FOUND);
        return Promise.reject(err);
      });
  },

  /**
   * List rtScanner in descending order of 'dateWatched' timestamp.
   * @param {number} skip - Number of users to be skipped.
   * @param {number} limit - Limit number of users to be returned.
   * @returns {Promise<User[]>}
   */
  list({ skip = 0, limit = 50 } = {}) {
    return this.find()
      .sort({ dateWatched: -1 })
      .skip(+skip)
      .limit(+limit)
      .exec();
  },

  speedrate() {
    const hour = moment().startOf('hour');
    const ltDateIso = hour.toISOString();
    const gtDateIso = hour.subtract(24, 'hours').toISOString();
    return this.aggregate([
      {
        $match: {
          dateWatched: {
            $gte: new Date(gtDateIso),
            $lt: new Date(ltDateIso)
          }
        }
      },
      {
        $project: {
          dateWatched: { $dateToString: { format: '%Y-%m-%dT%H', date: '$dateWatched' } },
          status: '$status',
        }
      },
      {
        $group: {
          _id: '$dateWatched',
          status: {
            $push: '$status'
          }
        }
      }
    ]).exec();
  },

};

mongoose.set('debug', true);
/**
 * @typedef Scanner
 */
export default mongoose.model('RtScanner', RtScannerSchema);
