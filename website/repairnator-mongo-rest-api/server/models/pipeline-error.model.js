/* eslint-disable no-trailing-spaces */
import Promise from 'bluebird';
import mongoose from 'mongoose';
import httpStatus from 'http-status';
import APIError from '../helpers/APIError';


/**
 * PipelineError Schema
 */
const PipelineErrorSchema = new mongoose.Schema({
  runId: String,
  hostname: String,
  buildId: Number,
  repositoryName: String,
  computedDate: Date,
  computedDateStr: String,
  computedDay: String,
}, { collection: 'pipeline-errors', strict: false }); // The collection doesn't have any strict schema.
/**
 * Add your
 * - pre-save hooks
 * - validations
 * - virtuals
 */

/**
 * Methods
 */
PipelineErrorSchema.method({
});

/**
 * Statics
 */
PipelineErrorSchema.statics = {
  /**
   * Get inspector
   * @param {ObjectId} id - The objectId of user.
   * @returns {Promise<User, APIError>}
   */
  get(id) {
    return this.findOne({ buildId: id })
       .exec()
       .then((pipelineError) => {
         if (pipelineError) {
           return pipelineError;
         }
         const err = new APIError('No such pipelineError data exists!', httpStatus.NOT_FOUND);
         return Promise.reject(err);
       });
  },

  list() {
    return this.find({ })
      .exec();
  },
};

mongoose.set('debug', true);
/**
 * @typedef PipelineError
 */
export default mongoose.model('PipelineError', PipelineErrorSchema);
