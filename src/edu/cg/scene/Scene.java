package edu.cg.scene;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.cg.Logger;
import edu.cg.algebra.Hit;
import edu.cg.algebra.Ops;
import edu.cg.algebra.Point;
import edu.cg.algebra.Ray;
import edu.cg.algebra.Vec;
import edu.cg.scene.camera.PinholeCamera;
import edu.cg.scene.lightSources.Light;
import edu.cg.scene.objects.Surface;

public class Scene {
	private String name = "scene";
	private int maxRecursionLevel = 1;
	private int antiAliasingFactor = 1; //gets the values of 1, 2 and 3
	private boolean renderRefractions = false;
	private boolean renderReflections = false;
	
	private PinholeCamera camera;
	private Vec ambient = new Vec(0.1, 0.1, 0.1); //white
	private Vec backgroundColor = new Vec(0, 0.5, 1); //blue sky
	private List<Light> lightSources = new LinkedList<>();
	private List<Surface> surfaces = new LinkedList<>();
	
	
	//MARK: initializers
	public Scene initCamera(Point eyePoistion, Vec towardsVec, Vec upVec,  double distanceToPlain) {
		this.camera = new PinholeCamera(eyePoistion, towardsVec, upVec,  distanceToPlain);
		return this;
	}

	public Scene initCamera(PinholeCamera pinholeCamera) {
		this.camera = pinholeCamera;
		return this;
	}
	
	public Scene initAmbient(Vec ambient) {
		this.ambient = ambient;
		return this;
	}
	
	public Scene initBackgroundColor(Vec backgroundColor) {
		this.backgroundColor = backgroundColor;
		return this;
	}
	
	public Scene addLightSource(Light lightSource) {
		lightSources.add(lightSource);
		return this;
	}
	
	public Scene addSurface(Surface surface) {
		surfaces.add(surface);
		return this;
	}
	
	public Scene initMaxRecursionLevel(int maxRecursionLevel) {
		this.maxRecursionLevel = maxRecursionLevel;
		return this;
	}
	
	public Scene initAntiAliasingFactor(int antiAliasingFactor) {
		this.antiAliasingFactor = antiAliasingFactor;
		return this;
	}
	
	public Scene initName(String name) {
		this.name = name;
		return this;
	}
	
	public Scene initRenderRefractions(boolean renderRefractions) {
		this.renderRefractions = renderRefractions;
		return this;
	}
	
	public Scene initRenderReflections(boolean renderReflections) {
		this.renderReflections = renderReflections;
		return this;
	}
	
	//MARK: getters
	public String getName() {
		return name;
	}
	
	public int getFactor() {
		return antiAliasingFactor;
	}
	
	public int getMaxRecursionLevel() {
		return maxRecursionLevel;
	}
	
	public boolean getRenderRefractions() {
		return renderRefractions;
	}
	
	public boolean getRenderReflections() {
		return renderReflections;
	}
	
	@Override
	public String toString() {
		String endl = System.lineSeparator(); 
		return "Camera: " + camera + endl +
				"Ambient: " + ambient + endl +
				"Background Color: " + backgroundColor + endl +
				"Max recursion level: " + maxRecursionLevel + endl +
				"Anti aliasing factor: " + antiAliasingFactor + endl +
				"Light sources:" + endl + lightSources + endl +
				"Surfaces:" + endl + surfaces;
	}
	
	private transient ExecutorService executor = null;
	private transient Logger logger = null;


	private void initSomeFields(int imgWidth, int imgHeight, double planeWidth, Logger logger) {
		this.logger = logger;
	}
	
	
	public BufferedImage render(int imgWidth, int imgHeight, double planeWidth ,Logger logger)
			throws InterruptedException, ExecutionException, IllegalArgumentException {
		
		initSomeFields(imgWidth, imgHeight, planeWidth, logger);
		
		BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
		camera.initResolution(imgHeight, imgWidth, planeWidth);
		int nThreads = Runtime.getRuntime().availableProcessors();
		nThreads = nThreads < 2 ? 2 : nThreads;
		this.logger.log("Initialize executor. Using " + nThreads + " threads to render " + name);
		executor = Executors.newFixedThreadPool(nThreads);
		
		@SuppressWarnings("unchecked")
		Future<Color>[][] futures = (Future<Color>[][])(new Future[imgHeight][imgWidth]);
		
		this.logger.log("Starting to shoot " +
			(imgHeight*imgWidth*antiAliasingFactor*antiAliasingFactor) +
			" rays over " + name);
		
		for(int y = 0; y < imgHeight; ++y)
			for(int x = 0; x < imgWidth; ++x)
				futures[y][x] = calcColor(x, y);
		
		this.logger.log("Done shooting rays.");
		this.logger.log("Wating for results...");
		
		for(int y = 0; y < imgHeight; ++y)
			for(int x = 0; x < imgWidth; ++x) {
				Color color = futures[y][x].get();
				img.setRGB(x, y, color.getRGB());
			}
		
		executor.shutdown();
		
		this.logger.log("Ray tracing of " + name + " has been completed.");
		
		executor = null;
		this.logger = null;
		
		return img;
	}
	
	private Future<Color> calcColor(int x, int y) {
        return executor.submit(() -> {
			Point pointOnScreen = camera.transform(x, y);
			Vec color = new Vec(0.0);

			Ray ray = new Ray(camera.getCameraPosition(), pointOnScreen);
			color = color.add(calcColor(ray, 0));

			return color.toColor();
			// TODO: change this method for AntiAliasing bonus
			//		You need to shoot antiAliasingFactor-1 additional rays through the pixel return the average color of
			//      all rays.
		});
	}
	
	private Vec calcColor(Ray ray, int recursionLevel) {
		if (recursionLevel > maxRecursionLevel) {
            return new Vec();
        }

        Hit hit = findIntersection(ray);
        if (hit == null) {
            // No intersection, returning background
            return this.backgroundColor;
        }

        Point surfaceHitPoint = ray.getHittingPoint(hit);
        Surface surface = hit.getSurface();
        Vec color = surface.Ka().mult(this.ambient);

        for (Light lightSource :
                lightSources) {

            if (isOccluded(surfaceHitPoint, lightSource)) continue;

            color = color.add(
                    getColorToAdd(ray,
                            hit,
                            surfaceHitPoint,
                            lightSource));
        }

        if (renderReflections && surface.isReflecting()) {

            Ray reflectedRay = new Ray(
                    surfaceHitPoint,
                    Ops.reflect(ray.direction(), hit.getNormalToSurface())
            );

            color = color.add(
                    calcColor(reflectedRay, recursionLevel + 1));
        }

        if (renderRefractions && surface.isTransparent()) {

            Ray refractedRay = new Ray(
                    surfaceHitPoint,
                    Ops.refract(ray.direction(),
                            hit.getNormalToSurface(),
                            surface.n1(hit), surface.n2(hit))
            );

            color = color.add(
                    calcColor(refractedRay, recursionLevel + 1));

        }
        return color;
	}

	private Vec getColorToAdd (Ray ray, Hit hit, Point surfaceHitPoint, Light lightSource) {
        Ray rayToLight = lightSource.rayToLight(surfaceHitPoint);

        Vec dirToLight =
                rayToLight.direction();

        // calc diffused: K_D * (N * L_i)
        Vec colorToAdd = calcDiffused(dirToLight, hit);

        // calc specular: K_S * ( V * R_i)^n
        colorToAdd = colorToAdd.add(calcSpecular(ray, dirToLight, hit));

        // add intensity: (specular + diffused) * I_L_i
        colorToAdd = lightSource.intensity(surfaceHitPoint, rayToLight).mult(colorToAdd);

        return colorToAdd;
    }

    private Vec calcSpecular (Ray ray, Vec dirToLight, Hit hit) {
        Vec normal = hit.getNormalToSurface();
        Vec reflectedLight = Ops.reflect(
                dirToLight.neg(),
                normal);

        Vec toViewer = ray.direction().neg();
        Vec ks = hit.getSurface().Ks();
        int shininess = hit.getSurface().shininess();

        double cos = reflectedLight.dot(toViewer);
        return cos > 0.0 ?
                ks.mult(Math.pow(cos, shininess))
                : new Vec();
    }

    private Vec calcDiffused (Vec dirToLight, Hit hit) {
        Vec normal = hit.getNormalToSurface();
        Vec kd = hit.getSurface().Kd();
        return kd.mult(Math.max(dirToLight.dot(normal), 0.0));
    }

    private boolean isOccluded (Point hitPoint, Light lightSource) {
        Ray rayToLight = lightSource.rayToLight(hitPoint);
        return surfaces.stream().anyMatch(surface -> lightSource.isOccludedBy(surface, rayToLight));
    }

    private Hit findIntersection (Ray ray) {
        Hit minHit = null;
        for (Surface surface : this.surfaces) {
            Hit newHit = surface.intersect(ray);
            if (minHit == null || (newHit != null && newHit.compareTo(minHit) < 0)) {
                minHit = newHit;
            }
        }
        return minHit;
    }
}
