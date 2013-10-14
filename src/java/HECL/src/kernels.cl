  
    kernel void calc_hist(read_only image2d_t input, int imgHeight, global float* histogram, int histSize) {
    	
    	//CLK_FILTER_NEAREST - Parecido a lo de bilinear filtering
    	//CLK_ADDRESS_CLAMP - out-of-range image coordinates will return a border color.
    	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE|CLK_ADDRESS_CLAMP|CLK_FILTER_NEAREST; 
    
    	int x = get_global_id(0); 
        
    	for(int y = 0; y < imgHeight; y++)
    	{
			int2 coord = (int2)(x,y); 
			float4 pixel_value = read_imagef(input, sampler, coord);
			
			int bin = round(pixel_value.x * (histSize - 1));
			
			if(bin < histSize)
				histogram[bin] = 1;
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
