/* eslint-disable no-trailing-spaces */
import Promise from 'bluebird';
import mongoose from 'mongoose';
import httpStatus from 'http-status';
import moment from 'moment';
import APIError from '../helpers/APIError';

/**
 * Inspector Schema
 */
const InspectorSchema = new mongoose.Schema({
  buildId: Number,
  repositoryName: String,
  status: String,
  prNumber: String,
  buildFinishedDate: String,
  buildFinishedDay: String,
  realStatus: String,
  hostname: String,
  buildReproductionDate: String,
  travisURL: String,
  typeOfFailures: String,
  runId: String
}, { collection: 'inspector' });

/**
 * Add your
 * - pre-save hooks
 * - validations
 * - virtuals
 */

/**
 * Methods
 */
InspectorSchema.method({
});

/**
 * Statics
 */
InspectorSchema.statics = {
  /**
   * Get inspector
   * @param {ObjectId} id - The objectId of user.
   * @returns {Promise<User, APIError>}
   */
  get(id) {
    return this.findOne({
      buildId: id
    })
      .exec()
      .then((inspector) => {
        if (inspector) {
          return inspector;
        }
        const err = new APIError('No such inspector data exists!', httpStatus.NOT_FOUND);
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
      .sort({ buildFinishedDate: -1 })
      .skip(+skip)
      .limit(+limit)
      .exec();
  },

  hostnameStats() {
    return this.aggregate([
      {
        $group: {
          _id: '$hostname',
          counted: {
            $sum: 1
          }
        }
      }
    ]).exec();
  },

  reproducedErrors(nbDays) {
    const ltDateIso = moment().toISOString();
    const gtDateIso = (nbDays !== 0) ? moment().subtract(nbDays, 'days').toISOString() : moment('01-02-2017', 'DD-MM-YYYY').toISOString();

    return this.aggregate([
      {
        $match: {
          buildFinishedDate: {
            $gte: new Date(gtDateIso),
            $lt: new Date(ltDateIso)
          },
          status: {
            $in: ['PATCHED', 'test errors', 'test failure']
          }
        }
      },
      {
        $project: {
          dayOfReproduction: { $dateToString: { format: '%Y-%m-%d', date: '$buildFinishedDate' } }
        }
      },
      {
        $group: {
          _id: '$dayOfReproduction',
          counted: {
            $sum: 1
          }
        }
      },
      {
        $sort: {
          _id: 1
        }
      }
    ]).exec();
  },

  statusStats(nbDays) {
    const ltDateIso = moment().toISOString();
    const gtDateIso = (nbDays !== 0) ? moment().subtract(nbDays, 'days').toISOString() : moment('01-01-2017', 'DD-MM-YYYY').toISOString();

    return this.aggregate([
      {
        $match: {
          buildFinishedDate: {
            $gte: new Date(gtDateIso),
            $lt: new Date(ltDateIso)
          },
        }
      },
      {
        $group: {
          _id: '$status',
          counted: {
            $sum: 1
          }
        }
      }
    ]).exec();
  },

  nbUniqueBuilds() {
    return this.aggregate([
      {
        $group: {
          _id: 'allBuilds',
          buildIds: {
            $addToSet: '$buildId'
          }
        }
      }, {
        $unwind: '$buildIds'
      }, {
        $group: {
          _id: 'nbBuilds',
          count: {
            $sum: 1
          }
        }
      }
    ]).exec();
  },

  getPatches() {
    return this.find({
      status: 'PATCHED'
    }).sort({
      buildReproductionDate: -1
    }).exec();
  },

  nbFailuresByProject() {
    return this.aggregate([
      {
        $addFields: {
          isPR: { $cond: { if: { $gt: ['$prNumber', 0] }, then: 1, else: 0 } }
        }
      },
      {
        $group: {
          _id: '$repositoryName',
          count: { $sum: 1 },
          nbPR: { $sum: '$isPR' }
        }
      },
      {
        $sort: {
          count: -1
        }
      }
    ]).exec();
  },

  nbReproducedByProject() {
    return this.aggregate([
      {
        $match: {
          status: {
            $in: ['PATCHED', 'test errors', 'test failure']
          }
        }
      },

      // Stage 2
      {
        $group: {
          _id: '$repositoryName',
          count: { $sum: 1 }
        }
      },

      // Stage 3
      {
        $sort: {
          count: -1
        }
      }
    ]).exec();
  },

  countReproducedErrors() {
    return this.count({
      status: {
        $in: ['PATCHED', 'test errors', 'test failure']
      }
    }).exec();
  }
};

/**
 * @typedef Inspector
 */
export default mongoose.model('Inspector', InspectorSchema);
