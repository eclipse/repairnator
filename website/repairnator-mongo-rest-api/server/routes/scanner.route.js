import express from 'express';
// import validate from 'express-validation';
// import paramValidation from '../../config/param-validation';
import scannerCtrl from '../controllers/scanner.controller';

const router = express.Router(); // eslint-disable-line new-cap

router.route('/')
  .get(scannerCtrl.list);

router.route('/count')
  .get(scannerCtrl.count);

router.route('/monthData')
  .get(scannerCtrl.monthData);

router.route('/weeksData/:nbWeeks')
  .get(scannerCtrl.weeksData);

router.param('nbWeeks', (req, res, next, nbWeeks) => {
  req.nbWeeks = nbWeeks; // eslint-disable-line no-param-reassign
  next();
});

router.route('/:scannerId')
  .get(scannerCtrl.get);

/** Load user when API with userId route parameter is hit */
router.param('scannerId', scannerCtrl.load);

export default router;
