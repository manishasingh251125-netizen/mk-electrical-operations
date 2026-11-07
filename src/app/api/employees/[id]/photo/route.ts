import {NextResponse} from "next/server";
import {requireOwner} from "@/lib/auth";
import {adminClient} from "@/lib/supabase/admin";
export async function POST(req:Request,{params}:{params:Promise<{id:string}>}){
 const guard=await requireOwner();if("error" in guard)return guard.error;
 const {id}=await params;const form=await req.formData();const file=form.get("photo");
 if(!(file instanceof File))return NextResponse.json({error:"Choose an employee photo."},{status:400});
 const allowed=["image/jpeg","image/png","image/webp"];if(!allowed.includes(file.type))return NextResponse.json({error:"Use a JPG, PNG or WebP image."},{status:400});
 if(file.size>3*1024*1024)return NextResponse.json({error:"Photo must be 3 MB or smaller."},{status:400});
 const admin=adminClient();const bucket="employee-photos";
 const {data:existing}=await admin.storage.getBucket(bucket);
 if(!existing){const {error}=await admin.storage.createBucket(bucket,{public:false,fileSizeLimit:3*1024*1024,allowedMimeTypes:allowed});if(error&&!error.message.toLowerCase().includes("already"))return NextResponse.json({error:error.message},{status:500});}
 const {data:profile,error:profileError}=await admin.from("profiles").select("id,photo_path").eq("id",id).maybeSingle();
 if(profileError||!profile)return NextResponse.json({error:"Employee not found."},{status:404});
 const ext=file.type==="image/png"?"png":file.type==="image/webp"?"webp":"jpg";const path=id+"/"+Date.now()+"."+ext;
 const {error:uploadError}=await admin.storage.from(bucket).upload(path,file,{contentType:file.type,upsert:false});
 if(uploadError)return NextResponse.json({error:uploadError.message},{status:400});
 const {error:updateError}=await admin.from("profiles").update({photo_path:path}).eq("id",id);
 if(updateError){await admin.storage.from(bucket).remove([path]);return NextResponse.json({error:updateError.message},{status:400});}
 if(profile.photo_path)await admin.storage.from(bucket).remove([profile.photo_path]);
 const {data:signed,error:signedError}=await admin.storage.from(bucket).createSignedUrl(path,3600);
 if(signedError)return NextResponse.json({error:signedError.message},{status:500});
 return NextResponse.json({ok:true,photo_path:path,photo_url:signed.signedUrl});
}
