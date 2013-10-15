kernel void equalize_image(read_only image2d_t input, write_only image2d_t output, global float* cdf){
	//CLK_FILTER_NEAREST - Parecido a lo de bilinear filtering
	//CLK_ADDRESS_CLAMP - out-of-range image coordinates will return a border color.
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE|CLK_ADDRESS_CLAMP|CLK_FILTER_NEAREST; 

	int x = get_global_id(0);
	int y = get_global_id(1);

	int2 coord = (int2)(x,y); 
	float4 pixel_value = read_imagef(input, sampler, coord);

	float4 eq_pixel_value = cdf[(int)pixel_value.x];

	write_imagef(output, coord, eq_pixel_value); 

}

kernel void merge_colHist(global int* colHistograms, global int* histogram, int colCount, int histSize){
	// There's one thread per histogram entry:
	int x = get_global_id(0);

	for(int i = 0; i < colCount; i++)
		histogram[x] += colHistograms[histSize * i + x];
}

kernel void calc_colHist(read_only image2d_t input, int imgHeight, global int* histogram, int histSize) {

	//CLK_FILTER_NEAREST - Parecido a lo de bilinear filtering
	//CLK_ADDRESS_CLAMP - out-of-range image coordinates will return a border color.
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE|CLK_ADDRESS_CLAMP|CLK_FILTER_NEAREST; 

	int x = get_global_id(0); 

	for(int y = 0; y < imgHeight; y++)
	{
		int2 coord = (int2)(x,y); 
		float4 pixel_value = read_imagef(input, sampler, coord);

		int bin = round(pixel_value.x);

		if(bin < histSize)
			histogram[x*histSize + bin]++;
	}
} 

kernel void convert_to_spherical(global float * image, const int width, const int height ) {

	//CLK_FILTER_NEAREST - Parecido a lo de bilinear filtering
	//CLK_ADDRESS_CLAMP - out-of-range image coordinates will return a border color.
	int x = get_global_id(0); 
	int y = get_global_id(1); 
	//int color = get_global_id(2); 
	int idx = x*height*3 + y*3;

	float R = image[idx]/255.0f;
	float G = image[idx + 1]/255.0f;
	float B = image[idx + 2]/255.0f;

	float r = sqrt(pow(R, 2.0f) + pow(G, 2.0f) + pow(B, 2.0f));
	float theta = acos(B/r);
	float phi = atan2(G, R);

	image[idx] = r;
	image[idx + 1] = theta;
	image[idx + 2] = phi;
} 


kernel void convert_to_rgb(global float * image, const int width, const int height ) {

	//CLK_FILTER_NEAREST - Parecido a lo de bilinear filtering
	//CLK_ADDRESS_CLAMP - out-of-range image coordinates will return a border color.
	int x = get_global_id(0); 
	int y = get_global_id(1); 
	//int color = get_global_id(2); 
	int idx = x*height*3 + y*3;

	float r = image[idx];
	float theta = image[idx + 1];
	float phi = image[idx + 2];

	float R = r*sin(theta)*cos(phi)*255.0;
	float G = r*sin(theta)*sin(phi)*255.0;
	float B = r*cos(theta)*255.0;

	image[idx] = R;
	image[idx + 1] = G;
	image[idx + 2] = B;
}    

kernel void convert_rgb_to_hsv(global float * image, const int width, const int height ) {

	//CLK_FILTER_NEAREST - Parecido a lo de bilinear filtering
	//CLK_ADDRESS_CLAMP - out-of-range image coordinates will return a border color.
	int x = get_global_id(0); 
	int y = get_global_id(1); 
	//int color = get_global_id(2); 
	int idx = x*height*3 + y*3;

	float R = image[idx]/255.0f;
	float G = image[idx + 1]/255.0f;
	float B = image[idx + 2]/255.0f;

	float L_b = max(max(R, G), B);
	float L_s = min(min(R, G), B);
	float V = 255.0f * L_b;
	float S = 0.0f;
	if(L_b > 0) 
	{
		S = (L_b - L_s)/L_b;
	}
	float H = 0.0f;
	
	if(L_b == L_s)
		H = 0.0f;
	else if(L_b == R && G >= B)
		H = M_PI*(G - B)/(3.0f*(L_b - L_s));
	else if(L_b == R && G < B)
		H = 2.0f*M_PI + M_PI*(G - B)/(3.0f*(L_b - L_s));
	else if(L_b == G)
		H = 2.0f*M_PI/3.0f + M_PI*(B - R)/(3*(L_b - L_s));
	else if(L_b == B)
		H = 4.0f*M_PI/3.0f + M_PI*(R - G)/(3*(L_b - L_s));


	image[idx] = H;
	image[idx + 1] = S;
	image[idx + 2] = V;
} 


kernel void convert_hsv_to_rgb(global float * image, const int width, const int height ) {

	//CLK_FILTER_NEAREST - Parecido a lo de bilinear filtering
	//CLK_ADDRESS_CLAMP - out-of-range image coordinates will return a border color.
	int x = get_global_id(0); 
	int y = get_global_id(1); 
	//int color = get_global_id(2); 
	int idx = x*height*3 + y*3;

	float H = image[idx];
	float S = image[idx + 1];
	float V = image[idx + 2];
	float V_p = V/255.0f; 
			
	float h = (int)(floor((3*H/M_PI)))%6;
	float f = 3*H/M_PI - h;
	float p = V_p*(1-S);
	float q = V_p*(1 - f*S);
	float t = V_p*(1 - (1 - f) * S);
	float R = 0.0f;
	float G = 0.0f;
	float B = 0.0f;
	
	if(h == 0) 
	{
		R = 255.0f * V_p;
		G = 255.0f*t;
		B = 255.0f*p;
	}
	else if(h == 1)
	{
		R = 255.0f * 1;
		G = 255.0f*V_p;
		B = 255.0f*p;
	}
	else if(h == 2)
	{
		R = 255.0f * p;
		G = 255.0f* V_p;
		B = 255.0f* t;
	}
	else if(h == 3)
	{
		R = 255.0f * p;
		G = 255.0f*q;
		B = 255.0f*V_p;
	}
	else if(h == 4)
	{
		R = 255.0f * t;
		G = 255.0f* p;
		B = 255.0f* V_p;
	}
	else if(h == 5)
	{
		R = 255.0f * V_p;
		G = 255.0f* p;
		B = 255.0f* q;
	}
	

	image[idx] = R;
	image[idx + 1] = G;
	image[idx + 2] = B;
}    


kernel void copy_image(read_only image2d_t input, write_only image2d_t output, const int width, const int height) {

	//CLK_FILTER_NEAREST - Parecido a lo de bilinear filtering
	//CLK_ADDRESS_CLAMP - out-of-range image coordinates will return a border color.
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE|CLK_ADDRESS_CLAMP|CLK_FILTER_NEAREST; 

	int x = get_global_id(0); 
	int y = get_global_id(1); 

	int2 coord = (int2)(x,y); 
	float4 pixel_value = read_imagef(input, sampler, coord);	
	write_imagef(output, coord, pixel_value); 
} 
