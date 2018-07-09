//
//  WBNode.h
//  Ring
//
//  Created by chris on 7/5/18.
//  Copyright © 2018 Warebots, LLC. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface WBNode : NSObject

- (instancetype) initWithAppGroupName:(NSString *) appGroupName andQueue:(dispatch_queue_t)queue;
- (void) readData:(void (^)(BOOL success, NSDictionary *output))completionBlock;
- (void) writeData:(int)version data:(NSString *)dataString withCompletion:(void (^)(BOOL success, NSDictionary *output))completionBlock;
- (int) getVersion;
- (BOOL) assertFilesExist;

@property NSString *appGroupName;
@property NSURL *containerUrl;
@property NSString *containerPath;
@property NSURL *sharedUrl;
@property NSString *sharedPath;
@property NSURL *dataUrl;
@property NSString *dataPath;
@property NSUserDefaults *defaults;
@property dispatch_queue_t queue;
@end
