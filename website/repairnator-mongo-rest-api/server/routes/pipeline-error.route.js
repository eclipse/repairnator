import express from 'express';
// import validate from 'express-validation';
// import paramValidation from '../../config/param-validation';
import pipelineErrorCtrl from '../controllers/pipeline-error.controller';

const router = express.Router(); // eslint-disable-line new-cap

router.route('/')
  .get(pipelineErrorCtrl.list);


/** This should remain at the end of the file */
router.route('/:pipelineErrorId')
  .get(pipelineErrorCtrl.get);

router.param('pipelineErrorId', pipelineErrorCtrl.load);

export default router;
