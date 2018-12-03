/* eslint-disable no-trailing-spaces */
import Promise from 'bluebird';
import mongoose from 'mongoose';
import httpStatus from 'http-status';
import APIError from '../helpers/APIError';

/**
 * Scanner Schema
 */
const PatchSchema = new mongoose.Schema({
  date: Date,
  dateStr: Date,
  runId: Date,
  buildId: Number,
  toolname: String,
  diff: String,
  filepath: String,
  hostname: String,
}, { collection: 'patches', strict: false });

/**
 * Add your
 * - pre-save hooks
 * - validations
 * - virtuals
 */

/**
 * Methods
 */
PatchSchema.method({
});

/**
 * Statics
 */
PatchSchema.statics = {
  /**
   * Get patch
   * @param {ObjectId} id
   * @returns {Promise<Patch, APIError>}
   */
  get(id) {
    return this.findById(id)
      .exec()
      .then((patch) => {
        if (patch) {
          return patch;
        }
        const err = new APIError('No such patch data exists!', httpStatus.NOT_FOUND);
        return Promise.reject(err);
      });
  },

  /**
   * List patches in descending order of 'date' timestamp.
   * @param {number} skip - Number of patches to be skipped.
   * @param {number} limit - Limit number of patches to be returned.
   * @returns {Promise<Patch[]>}
   */
  list({ skip = 0, limit = 50 } = {}) {
    return this.find()
      .sort({ dateStr: -1 })
      .skip(+skip)
      .limit(+limit)
      .exec();
  },

  listBuildPatches(buildId) {
    return this.find({ buildId })
      .exec();
  },

};

mongoose.set('debug', true);
/**
 * @typedef Scanner
 */
export default mongoose.model('Patch', PatchSchema);
