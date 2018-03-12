import express from 'express';
// import validate from 'express-validation';
// import paramValidation from '../../config/param-validation';
import inspectorCtrl from '../controllers/inspector.controller';

const router = express.Router(); // eslint-disable-line new-cap

router.route('/')
  .get(inspectorCtrl.list);

router.route('/count')
  .get(inspectorCtrl.count);

router.route('/countSuccessfullyReproducedBuilds')
  .get(inspectorCtrl.countSuccessFullyReproducedBuild);

router.route('/hostnameStats')
  .get(inspectorCtrl.hostnameStats);

router.route('/statusStats')
  .get(inspectorCtrl.statusStats);

router.route('/statusStats/:nbDays')
  .get(inspectorCtrl.statusStatsPeriod);

router.route('/reproducedBuilds')
  .get(inspectorCtrl.reproducedBuildsAll);

router.route('/reproducedBuilds/:nbDays')
  .get(inspectorCtrl.reproducedBuilds);

router.param('nbDays', (req, res, next, nbDays) => {
  req.nbDays = nbDays; // eslint-disable-line no-param-reassign
  next();
});

router.route('/uniqueBuilds')
  .get(inspectorCtrl.nbUniqueBuilds);

router.route('/patches')
  .get(inspectorCtrl.getPatches);

router.route('/failuresByProject')
  .get(inspectorCtrl.getNbFailuresByProject);

router.route('/reproducedByProject')
  .get(inspectorCtrl.getNbReproducedByProject);


/** This should remain at the end of the file */
router.route('/:inspectorId')
  .get(inspectorCtrl.get);

router.param('inspectorId', inspectorCtrl.load);

export default router;
