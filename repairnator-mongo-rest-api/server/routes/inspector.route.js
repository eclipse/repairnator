import express from 'express';
// import validate from 'express-validation';
// import paramValidation from '../../config/param-validation';
import inspectorCtrl from '../controllers/inspector.controller';

const router = express.Router(); // eslint-disable-line new-cap

router.route('/')
  /** GET /api/users - Get list of users */
  .get(inspectorCtrl.list);

router.route('/count')
  .get(inspectorCtrl.count);

router.route('/hostnameStats')
  .get(inspectorCtrl.hostnameStats);

router.route('/uniqueBuilds')
  .get(inspectorCtrl.nbUniqueBuilds);

router.route('/:inspectorId')
  /** GET /api/users/:userId - Get user */
  .get(inspectorCtrl.get);

/** Load user when API with userId route parameter is hit */
router.param('inspectorId', inspectorCtrl.load);

export default router;
