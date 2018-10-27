/* eslint-disable no-trailing-spaces */
import Promise from 'bluebird';
import mongoose from 'mongoose';
import httpStatus from 'http-status';
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

};

mongoose.set('debug', true);
/**
 * @typedef Scanner
 */
export default mongoose.model('RtScanner', RtScannerSchema);
