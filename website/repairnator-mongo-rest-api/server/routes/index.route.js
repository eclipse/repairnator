import express from 'express';
import inspectorRoutes from './inspector.route';
import scannerRoutes from './scanner.route';
import rtScannerRoutes from './rtscanner.route';
import pipelineErrorRoutes from './pipeline-error.route';
import patchRoutes from './patch.route';

const router = express.Router(); // eslint-disable-line new-cap

/** GET /health-check - Check service health */
router.get('/health-check', (req, res) =>
  res.send('OK')
);

// mount user routes at /users
router.use('/inspectors', inspectorRoutes);
router.use('/scanners', scannerRoutes);
router.use('/rtscanners', rtScannerRoutes);
router.use('/pipeline-errors', pipelineErrorRoutes);
router.use('/patches', patchRoutes);

export default router;
