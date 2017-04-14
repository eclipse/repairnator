import express from 'express';
import inspectorRoutes from './inspector.route';
import scannerRoutes from './scanner.route';
import authRoutes from './auth.route';

const router = express.Router(); // eslint-disable-line new-cap

/** GET /health-check - Check service health */
router.get('/health-check', (req, res) =>
  res.send('OK')
);

// mount user routes at /users
router.use('/inspectors', inspectorRoutes);
router.use('/scanners', scannerRoutes);

// mount auth routes at /auth
router.use('/auth', authRoutes);

export default router;
