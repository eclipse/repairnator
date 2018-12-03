import express from 'express';
import patchCtrl from '../controllers/patch.controller';

const router = express.Router(); // eslint-disable-line new-cap

router.route('/')
  .get(patchCtrl.list);

router.route('/:patchId')
  .get(patchCtrl.get);

router.route('/builds/:buildId')
  .get(patchCtrl.listBuildPatches);

/** Load user when API with userId route parameter is hit */
router.param('patchId', patchCtrl.load);

export default router;
