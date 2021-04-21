package edu.cg.scene;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.cg.Logger;
import edu.cg.UnimplementedMethodException;
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

	// TODO: add your fields here with the transient keyword
	//  for example - private transient Object myField = null;

	private void initSomeFields(int imgWidth, int imgHeight, double planeWidth, Logger logger) {
		this.logger = logger;
		// TODO: initialize your fields that you added to this class here.
		//      Make sure your fields are declared with the transient keyword
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
            Vec color = new Vec(0,0,0);
            for (int i = 0; i < antiAliasingFactor ; i++) {
                for (int j = 0; j < antiAliasingFactor ; j++) {
                    Point centerPoint = camera.transform(x * antiAliasingFactor + i, y * antiAliasingFactor + j);
                    Ray ray = new Ray(camera.getCameraPosition(), centerPoint);
                    color = color.add(calcColor(ray, 0));
                }
            }
            return color.mult(1.0 / Math.pow(this.antiAliasingFactor, 2)).toColor();
        });
	}
	
	private Vec calcColor(Ray ray, int recursionLevel) {
		if (recursionLevel == maxRecursionLevel) {
            return new Vec();
        }

        Optional<Hit> optHit = findIntersection(ray);
        if (optHit.isEmpty()) {
            // No intersection, returing bgcolor
            return this.backgroundColor;
        }

        Hit hit = optHit.get();
        // Calculate ambient color for vector, K_a * I_{amb}
        Point surfaceHitPoint = ray.getHittingPoint(hit);
        Surface surface = hit.getSurface();
        Vec color = surface.Ka().mult(this.ambient);

        for (Light lightSource :
                lightSources) {

            // is occluded, continue
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

        Vec directionToLight =
                rayToLight.direction();

        // calc diffused: K_D * (N * L_i)
        Vec colorToAdd = calcDiffused(directionToLight, hit);

        // calc specular: K_S * ( V * R_i)^n
        colorToAdd = colorToAdd.add(calcSpecular(ray, directionToLight, hit));

        // add intensity: (specular + diffused) * I_L_i
        colorToAdd = lightSource.intensity(surfaceHitPoint, rayToLight).mult(colorToAdd);

        return colorToAdd;
    }

    private Vec calcSpecular (Ray ray, Vec directionToLight, Hit hit) {

        Vec normal = hit.getNormalToSurface();

        // We take the ray from the point to the light source (L.neg()) and
        // reflect it
        Vec reflectedLightToViewerDirection = Ops.reflect(
                directionToLight.neg(),
                normal);

        Vec toViewer = ray.direction().neg();

        Vec Ks = hit.getSurface().Ks();
        int shininess = hit.getSurface().shininess();

        double cosine = reflectedLightToViewerDirection.dot(toViewer);

        // If the angle is greater than 90 degrees, the light
        // doesn't get to the viewer so we return 0
        return cosine > 0.0 ?
                Ks.mult(Math.pow(cosine, shininess))
                : new Vec();
    }

    private Vec calcDiffused (Vec directionToLight, Hit hit) {
        Vec normal = hit.getNormalToSurface();

        Vec Kd = hit.getSurface().Kd();

        // If the angle is greater than 90 degrees (N * L < 0), the light
        // doesn't hit the surface so we return 0
        return Kd.mult(Math.max(directionToLight.dot(normal), 0.0));
    }

    private boolean isOccluded (Point hitPoint, Light lightSource) {
        Ray rayToLight = lightSource.rayToLight(hitPoint);
        return surfaces.stream().anyMatch(surface -> lightSource.isOccludedBy(surface, rayToLight));
    }

    private Optional<Hit> findIntersection (Ray ray) {
        return surfaces.stream().
                map(surface -> surface.intersect(ray)).
                filter(Objects::nonNull).
                min(Hit::compareTo);
    }
}
