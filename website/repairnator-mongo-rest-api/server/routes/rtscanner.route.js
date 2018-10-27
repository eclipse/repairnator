import express from 'express';
// import validate from 'express-validation';
// import paramValidation from '../../config/param-validation';
import rtScannerCtrl from '../controllers/rtscanner.controller';

const router = express.Router(); // eslint-disable-line new-cap

router.route('/')
  .get(rtScannerCtrl.list);

router.route('/:rtScannerId')
  .get(rtScannerCtrl.get);

/** Load user when API with userId route parameter is hit */
router.param('rtScannerId', rtScannerCtrl.load);

export default router;
