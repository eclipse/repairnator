/* eslint-disable no-trailing-spaces */
import Promise from 'bluebird';
import mongoose from 'mongoose';
import httpStatus from 'http-status';
import APIError from '../helpers/APIError';

/**
 * Scanner Schema
 */
const ScannerSchema = new mongoose.Schema({
  hostname: String,
  dateBegin: Date,
  dateLimit: Date,
  totalRepoNumber: Number,
  totalScannedBuilds: Number,
  totalJavaBuilds: Number,
  totalJavaPassingBuilds: Number,
  totalJavaFailingBuilds: Number,
  totalJavaFailingBuildsWithFailingTests: Number,
  totalPRBuilds: Number,
  dayLimit: String,
  duration: String,
  runId: String,
  dateBeginStr: String,
  dateLimitStr: String
}, { collection: 'scanner' });

/**
 * Add your
 * - pre-save hooks
 * - validations
 * - virtuals
 */

/**
 * Methods
 */
ScannerSchema.method({
});

/**
 * Statics
 */
ScannerSchema.statics = {
  /**
   * Get inspector
   * @param {ObjectId} id - The objectId of user.
   * @returns {Promise<User, APIError>}
   */
  get(id) {
    return this.findById(id)
      .exec()
      .then((inspector) => {
        if (inspector) {
          return inspector;
        }
        const err = new APIError('No such scanner data exists!', httpStatus.NOT_FOUND);
        return Promise.reject(err);
      });
  },

  /**
   * List users in descending order of 'createdAt' timestamp.
   * @param {number} skip - Number of users to be skipped.
   * @param {number} limit - Limit number of users to be returned.
   * @returns {Promise<User[]>}
   */
  list({ skip = 0, limit = 50 } = {}) {
    return this.find()
      .sort({ dateBegin: -1 })
      .skip(+skip)
      .limit(+limit)
      .exec();
  },

  getMonthData(month, year) {
    const nextMonth = (month === 12) ? 1 : month + 1;
    const nextYear = (month === 12) ? year + 1 : year;

    const gtDate = new Date();
    gtDate.setUTCFullYear(year, month, 1);
    gtDate.setUTCHours(0, 0, 0);
    const gtDateIso = gtDate.toISOString();

    const ltDate = new Date();
    ltDate.setUTCFullYear(nextYear, nextMonth, 1);
    ltDate.setUTCHours(0, 0, 0);

    const ltDateIso = ltDate.toISOString();

    console.log("Isodate: "+gtDateIso+" and "+ltDateIso);

    return this.find({
      dateBegin: {
        $gte: gtDateIso,
        $lt: ltDateIso
      }
    }).sort({ dateBegin: 1 }).exec();
  }
};

mongoose.set('debug', true);
/**
 * @typedef Scanner
 */
export default mongoose.model('Scanner', ScannerSchema);
